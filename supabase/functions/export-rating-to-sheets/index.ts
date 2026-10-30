const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const jsonResponse = (status: number, payload: Record<string, unknown>) =>
  new Response(JSON.stringify(payload), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });

const base64Url = (input: Uint8Array) => {
  let binary = "";
  for (const byte of input) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
};

const encodeJson = (value: unknown) =>
  base64Url(new TextEncoder().encode(JSON.stringify(value)));

async function googleAccessToken(serviceAccount: { client_email: string; private_key: string }) {
  const now = Math.floor(Date.now() / 1000);
  const header = encodeJson({ alg: "RS256", typ: "JWT" });
  const claims = encodeJson({
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/spreadsheets",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  });
  const unsigned = `${header}.${claims}`;
  const pem = serviceAccount.private_key
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\s/g, "");
  const keyBytes = Uint8Array.from(atob(pem), (character) => character.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    "pkcs8",
    keyBytes,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = new Uint8Array(
    await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned)),
  );
  const assertion = `${unsigned}.${base64Url(signature)}`;
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) throw new Error("Google autentifikacija nepavyko.");
  const token = await response.json();
  if (typeof token.access_token !== "string") throw new Error("Google tokenas negautas.");
  return token.access_token as string;
}

function normalizedSheetName(name: string) {
  return name.replace(/[:\\/?*\[\]]/g, "_").slice(0, 99).trim() || "Vertinimai";
}

Deno.serve(async (request: Request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return jsonResponse(405, { ok: false, error: "Naudokite POST." });

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !anonKey || !serviceKey) {
    return jsonResponse(503, { ok: false, error: "Supabase funkcija nesukonfigūruota." });
  }

  const authorization = request.headers.get("Authorization") ?? "";
  const callerToken = authorization.replace(/^Bearer\s+/i, "");
  if (!callerToken || callerToken === anonKey || callerToken === serviceKey) {
    return jsonResponse(401, { ok: false, error: "Reikia prisijungti mokytojo paskyra." });
  }

  let ratingId: string;
  try {
    const body = await request.json();
    ratingId = typeof body.rating_id === "string" ? body.rating_id : "";
  } catch {
    return jsonResponse(400, { ok: false, error: "Netinkamas užklausos turinys." });
  }
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(ratingId)) {
    return jsonResponse(400, { ok: false, error: "Neteisingas vertinimo ID." });
  }

  const rest = async (path: string, init: RequestInit = {}) => {
    const response = await fetch(`${supabaseUrl}/rest/v1/${path}`, {
      ...init,
      headers: {
        apikey: serviceKey,
        Authorization: `Bearer ${serviceKey}`,
        "Content-Type": "application/json",
        ...init.headers,
      },
    });
    if (!response.ok) throw new Error("Supabase duomenų užklausa nepavyko.");
    return response.status === 204 ? null : await response.json();
  };

  let exportClaimed = false;
  try {
    const authResponse = await fetch(`${supabaseUrl}/auth/v1/user`, {
      headers: { apikey: anonKey, Authorization: authorization },
    });
    if (!authResponse.ok) return jsonResponse(401, { ok: false, error: "Sesija negalioja." });
    const user = await authResponse.json();
    const userId = typeof user.id === "string" ? user.id : "";
    if (!userId) return jsonResponse(401, { ok: false, error: "Sesija negalioja." });

    const ratings = await rest(
      `ratings?select=id,teacher_id,lesson_no,score,comment,rated_on,classes(name),profiles(id,full_name)&id=eq.${ratingId}&limit=1`,
    );
    const rating = Array.isArray(ratings) ? ratings[0] : undefined;
    if (!rating) return jsonResponse(404, { ok: false, error: "Vertinimas nerastas." });

    const profiles = await rest(`profiles?select=role,active&id=eq.${encodeURIComponent(userId)}&limit=1`);
    const profile = Array.isArray(profiles) ? profiles[0] : undefined;
    if (!profile?.active || !["teacher", "admin"].includes(profile.role)) {
      return jsonResponse(403, { ok: false, error: "Paskyra neaktyvi." });
    }
    if (rating.teacher_id !== userId && profile.role !== "admin") {
      return jsonResponse(403, { ok: false, error: "Negalite eksportuoti kito mokytojo vertinimo." });
    }

    const configs = await rest("google_sheets_config?select=enabled,spreadsheet_id,sheet_name,sheet_per_teacher&id=eq.true&limit=1");
    const config = Array.isArray(configs) ? configs[0] : undefined;
    if (!config?.enabled) return jsonResponse(200, { ok: true, exported: false, skipped: true });
    if (!config.spreadsheet_id || !config.sheet_name) {
      return jsonResponse(503, { ok: false, error: "Google Sheets nustatymai neužpildyti." });
    }

    const claimResponse = await fetch(`${supabaseUrl}/rest/v1/rpc/claim_google_sheets_export`, {
      method: "POST",
      headers: {
        apikey: anonKey,
        Authorization: authorization,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ p_rating_id: ratingId }),
    });
    if (!claimResponse.ok) throw new Error("Nepavyko rezervuoti eksporto.");
    const claims = await claimResponse.json();
    const claimed = claims === true;
    exportClaimed = claimed;
    if (!claimed) {
      const prior = await rest(`google_sheets_exports?select=status&rating_id=eq.${ratingId}&limit=1`);
      if (Array.isArray(prior) && prior[0]?.status === "exported") {
        return jsonResponse(200, { ok: true, exported: true, skipped: true });
      }
      return jsonResponse(409, { ok: false, error: "Šis vertinimas jau eksportuojamas; bandykite vėliau." });
    }

    const serviceAccountRaw = Deno.env.get("GOOGLE_SERVICE_ACCOUNT_JSON");
    if (!serviceAccountRaw) throw new Error("Google paskyros raktas Supabase paslaptyse nenustatytas.");
    const serviceAccount = JSON.parse(serviceAccountRaw);
    if (typeof serviceAccount.client_email !== "string" || typeof serviceAccount.private_key !== "string") {
      throw new Error("Google service account paslaptis netinkama.");
    }

    const teacher = Array.isArray(rating.profiles) ? rating.profiles[0] : rating.profiles;
    const klass = Array.isArray(rating.classes) ? rating.classes[0] : rating.classes;
    const teacherName = String(teacher?.full_name || "Mokytojas");
    const configuredTab = String(config.sheet_name);
    const tabName = normalizedSheetName(
      config.sheet_per_teacher ? `${configuredTab}-${teacherName}` : configuredTab,
    );
    const accessToken = await googleAccessToken(serviceAccount);
    const spreadsheetId = encodeURIComponent(String(config.spreadsheet_id));
    const sheetUrl = `https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId}`;
    const metadataResponse = await fetch(`${sheetUrl}?fields=sheets(properties(title))`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (!metadataResponse.ok) throw new Error("Google dokumento pasiekti nepavyko.");
    const metadata = await metadataResponse.json();
    const sheetExists = metadata.sheets?.some((sheet: { properties?: { title?: string } }) =>
      sheet.properties?.title === tabName
    );
    if (!sheetExists) {
      const addResponse = await fetch(`${sheetUrl}:batchUpdate`, {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify({ requests: [{ addSheet: { properties: { title: tabName } } }] }),
      });
      if (!addResponse.ok) throw new Error("Google dokumento lapo sukurti nepavyko.");
    }

    const quotedTab = `'${tabName.replace(/'/g, "''")}'`;
    const range = encodeURIComponent(`${quotedTab}!A1`);
    const firstCellResponse = await fetch(`${sheetUrl}/values/${range}`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (!firstCellResponse.ok) throw new Error("Google dokumento lapo perskaityti nepavyko.");
    const firstCell = await firstCellResponse.json();
    const row = [klass?.name ?? "", rating.score, rating.comment ?? "", teacherName, rating.rated_on, rating.lesson_no];
    const values = firstCell.values?.length
      ? [row]
      : [
        ["Klasė", "Mikroklimatas (1–5)", "Komentaras", "Mokytojo vardas", "Data", "Pamoka"],
        row,
      ];
    const appendResponse = await fetch(
      `${sheetUrl}/values/${encodeURIComponent(quotedTab)}:append?valueInputOption=RAW&insertDataOption=INSERT_ROWS`,
      {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify({ majorDimension: "ROWS", values }),
      },
    );
    if (!appendResponse.ok) throw new Error("Vertinimo į Google Sheets įrašyti nepavyko.");

    await rest(`google_sheets_exports?rating_id=eq.${ratingId}`, {
      method: "PATCH",
      headers: { Prefer: "return=minimal" },
      body: JSON.stringify({ status: "exported", updated_at: new Date().toISOString() }),
    });
    return jsonResponse(200, { ok: true, exported: true, skipped: false });
  } catch (error) {
    if (exportClaimed && supabaseUrl && serviceKey) {
      await fetch(`${supabaseUrl}/rest/v1/google_sheets_exports?rating_id=eq.${ratingId}`, {
        method: "PATCH",
        headers: {
          apikey: serviceKey,
          Authorization: `Bearer ${serviceKey}`,
          "Content-Type": "application/json",
          Prefer: "return=minimal",
        },
        body: JSON.stringify({ status: "failed", updated_at: new Date().toISOString() }),
      }).catch(() => undefined);
    }
    const message = error instanceof Error ? error.message : "Eksportuoti į Google Sheets nepavyko.";
    return jsonResponse(502, { ok: false, exported: false, error: message });
  }
});
