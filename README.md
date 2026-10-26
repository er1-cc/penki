# penki

Android programėlė mokytojams: po kiekvienos pamokos įvertinamas **klasės elgesys (mikroklimatas) nuo 1 iki 5**,
neprivalomai pridedamas komentaras. Duomenys saugomi **Supabase** (Postgres + prisijungimas + RLS),
administratorius mato visų mokytojų vertinimus ir **tik jis gali pakviesti mokytojus**.

Sąsaja – lietuvių kalba.

## Funkcijos

- **Mokytojas**
  - pamokų sąrašas šiandien (1–7 pamokos, standartiniai laikai),
  - vertinimas 1–5 (1 – labai blogas, 5 – labai geras) + neprivalomas komentaras,
  - klasės pasirinkimas, tos pačios pamokos vertinimo keitimas,
  - savo vertinimų istorija.
- **Administratorius**
  - mokytojų sąrašas: patvirtinimas, aktyvavimas / išjungimas, administratoriaus teisių suteikimas,
  - **kvietimai**: sukuriamas vienkartinis 6 simbolių kodas konkrečiam el. paštui (galioja 14 d.),
  - klasių sąrašo valdymas,
  - statistika: bendras vidurkis, vidurkiai pagal klases ir mokytojus, naujausi komentarai.
- **Pranešimai**
  - priminimas „Baigėsi N-a pamoka – įvertink klasės elgesį“ iškart pasibaigus pamokai,
  - veikia telefone esant užrakintam ir be interneto (AlarmManager, ne FCM),
  - priminimai atkuriami įjungus telefoną, siunčiami tik darbo dienomis,
  - galima išjungti Nustatymuose.

## Struktūra

```
android/                 Android programa (Kotlin + Jetpack Compose)
  app/src/main/java/lt/penki/
    data/                Supabase REST klientas, modeliai, sesija, pamokų laikai
    notify/              pranešimai (AlarmManager, boot receiver)
    ui/                  ekranai: prisijungimas, vertinimas, admin panelė, nustatymai
supabase/schema.sql      duomenų bazės schema, RLS taisyklės, kvietimo funkcija
artifacts/penki-1.1.apk  sukompiliuotas APK (debug parašas), Supabase reikšmės jau įrašytos
```

## 1. Supabase paruošimas

1. Susikurk projektą [supabase.com](https://supabase.com/dashboard) (rekomenduojama ES regionas).
2. **SQL Editor → New query** → įklijuok visą `supabase/schema.sql` → **Run**.
   Sukuriamos lentelės `profiles`, `classes`, `invites`, `ratings`, RLS taisyklės ir funkcija `redeem_invite`.
3. **Authentication → Sign In / Providers → Email**: išjunk **Confirm email**
   (kad mokytojas galėtų iš karto prisijungti po registracijos).
4. **Authentication → Users → Add user**: sukurk savo (administratoriaus) paskyrą su el. paštu ir slaptažodžiu.
5. SQL Editor paleisk (pakeisk el. paštą):

   ```sql
   update public.profiles set role = 'admin', active = true
   where email = 'admin@mokykla.lt';
   ```

6. **Settings → API**: nusikopijuok **Project URL** ir **anon public** raktą.

> Naudok tik `anon` (public) raktą. `service_role` (secret) rakto į programėlę niekada neįrašyk –
> jis apeina visas RLS taisykles, o iš APK jį gali ištraukti bet kas. Jei `service_role` raktas
> kada nors buvo išsiųstas ar įrašytas ne saugioje vietoje, pakeisk jį Supabase:
> **Settings → API → JWT Settings → Generate new keys**.

## 2. Programos prijungimas prie Supabase

**Šiame APK (`artifacts/penki-1.1.apk`) reikšmės jau įrašytos** – projektas
`https://ypnulvhcrhxcfpfbwath.supabase.co` ir `anon` raktas. Paleidus programą iškart rodomas
prisijungimo ekranas, nieko vesti nereikia.

Jei nori naudoti kitą Supabase projektą, pakeisk `android/gradle.properties`:

```properties
penki.supabaseUrl=https://tavo-projektas.supabase.co
penki.supabaseAnonKey=eyJhbGciOi...
```

ir perkompiliuok APK (žr. žemiau). Jei reikšmės tuščios, programa parodys ekraną, kuriame jas
galima įvesti ranka telefone (jos lieka tik tame telefone).

## 3. APK kompiliavimas

Reikia JDK 17 ir Android SDK (platform 34, build-tools 34.0.0).

**Android Studio:** atsidaryk `android/` katalogą ir rinkis *Build → Build Bundle(s)/APK(s) → Build APK(s)*.

**Komandinė eilutė:**

```bash
cd android
./gradlew :app:assembleDebug
# rezultatas: app/build/outputs/apk/debug/app-debug.apk
```

Release versijai (reikės savo parašo):

```bash
./gradlew :app:assembleRelease
```

## 4. Diegimas telefone

1. Nukopijuok `artifacts/penki-1.1.apk` į telefoną (arba atsiųsk per „Drive“ / el. paštą).
2. Atidaryk failą → Android paprašys leisti diegti iš šio šaltinio → **Leisti**.
3. Pirmą kartą paleidus: įvesk Supabase URL ir anon raktą (jei neįrašei į APK),
   tada prisijunk su administratoriaus paskyra.

APK yra su **debug parašu** – tinka diegti į telefoną ir testuoti, bet ne į „Google Play“.
„Play“ parduotuvei reikia release parašo (`signingConfigs`).

## 5. Darbo eiga mokykloje

1. Administratorius prisijungia → **Admin → Klasės**: surašo klases (pvz. `7b`, `8a`, `11`).
2. **Admin → Kvietimai**: įveda mokytojo el. paštą → gauna 6 simbolių kodą → nusiunčia mokytojui.
3. Mokytojas telefone: **Registruotis su kvietimo kodu** → įveda vardą, el. paštą, slaptažodį ir kodą.
   Paskyra iškart aktyvuojama (kodas vienkartinis, galioja 14 dienų).
4. Po kiekvienos pamokos ateina pranešimas. Paspaudus jį atsidaro vertinimo langas:
   pamoka, klasė, balas 1–5, neprivalomas komentaras.
5. Administratorius **Admin → Statistika** mato visų mokytojų vertinimus ir komentarus.

Jei mokytojas užsiregistravo be kvietimo kodo, paskyra lieka neaktyvi –
ją gali patvirtinti administratorius **Admin → Mokytojai → Patvirtinti mokytoju**.

## 6. Pranešimai ir baterijos taupymas

Pranešimai planuojami telefone, todėl:

- reikia leidimo „Pranešimai“ (Android 13+ programėlė paprašo pati);
- kai kurie gamintojai (Xiaomi, Huawei, Samsung) agresyviai stabdo fonines programas –
  programėlę verta įtraukti į „Neribotas baterijos naudojimas“;
- jei telefonas išjungtas / perkrautas, priminimai atstatomi automatiškai;
- pamokų laikai kol kas fiksuoti (8:45, 9:40, 10:35, 11:30, 12:25, 13:20, 14:15),
  keisti – `android/app/src/main/java/lt/penki/data/Lessons.kt`.

## Apribojimai (pirmoji versija)

- Pamokų laikai fiksuoti, savaitgaliais priminimai nesiunčiami.
- Nėra pamokų tvarkaraščio importo ir mokyklos prisijungimo per SSO.
- Vertinimas – vienas balsas vienai klasei vienai pamokai per dieną (pakartotinis įrašas atnaujina balą).
- Statistika skaičiuojama telefone iš paskutinių 500 vertinimų.
