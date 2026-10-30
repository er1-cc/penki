-- ============================================================
--  penki – klasės elgesio (mikroklimato) vertinimo programa
--  Naujos instaliacijos schema. Esamai instaliacijai taikyk .dev/penki_feature_migration.sql
-- ============================================================

create extension if not exists pgcrypto;

-- ---------- PROFILIAI ----------
create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    email text not null default '',
    full_name text not null default '',
    role text not null default 'pending' check (role in ('pending', 'teacher', 'admin')),
    active boolean not null default false,
    created_at timestamptz not null default now()
);

-- ---------- KLASES ----------
create table if not exists public.classes (
    id uuid primary key default gen_random_uuid(),
    name text not null unique,
    active boolean not null default true,
    created_at timestamptz not null default now()
);

-- ---------- KVIETIMAI ----------
create table if not exists public.invites (
    id uuid primary key default gen_random_uuid(),
    email text not null,
    code text not null unique,
    created_by uuid references public.profiles(id) on delete set null,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null default now() + interval '14 days',
    redeemed_at timestamptz,
    redeemed_by uuid references public.profiles(id) on delete set null
);

-- ---------- VERTINIMAI ----------
create table if not exists public.ratings (
    id uuid primary key default gen_random_uuid(),
    teacher_id uuid not null references public.profiles(id) on delete cascade,
    class_id uuid not null references public.classes(id) on delete cascade,
    lesson_no int not null check (lesson_no between 1 and 10),
    score int not null check (score between 1 and 5),
    comment text not null default '',
    rated_on date not null default (now() at time zone 'Europe/Vilnius')::date,
    created_at timestamptz not null default now()
);
create index if not exists ratings_rated_on_idx on public.ratings(rated_on desc);
create index if not exists ratings_teacher_idx on public.ratings(teacher_id);
create unique index if not exists ratings_unique_lesson_idx
    on public.ratings(teacher_id, class_id, rated_on, lesson_no);

-- ---------- MOKYTOJU KLASIU PRISKYRIMAS ----------
create table if not exists public.teacher_classes (
    teacher_id uuid not null references public.profiles(id) on delete cascade,
    class_id uuid not null references public.classes(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (teacher_id, class_id)
);

-- ---------- INDIVIDUALUS PAMOKU GRAFIKAI ----------
create table if not exists public.lesson_times (
    teacher_id uuid not null references public.profiles(id) on delete cascade,
    lesson_no int not null check (lesson_no between 1 and 10),
    ends_at time not null,
    updated_at timestamptz not null default now(),
    primary key (teacher_id, lesson_no)
);
insert into public.lesson_times (teacher_id, lesson_no, ends_at)
select p.id, defaults.lesson_no, defaults.ends_at
from public.profiles p
cross join (values (1,'08:45'::time),(2,'09:40'::time),(3,'10:35'::time),(4,'11:30'::time),
                   (5,'12:25'::time),(6,'13:20'::time),(7,'14:15'::time)) as defaults(lesson_no,ends_at)
where p.active and p.role in ('teacher','admin')
on conflict (teacher_id,lesson_no) do nothing;

-- Numatytieji laikai pirmame grafiko sukūrime.
create or replace function public.seed_teacher_lesson_times()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    if new.active and new.role in ('teacher','admin') then
        insert into public.lesson_times (teacher_id,lesson_no,ends_at) values
            (new.id,1,'08:45'),(new.id,2,'09:40'),(new.id,3,'10:35'),
            (new.id,4,'11:30'),(new.id,5,'12:25'),(new.id,6,'13:20'),(new.id,7,'14:15')
        on conflict (teacher_id,lesson_no) do nothing;
    end if;
    return new;
end;
$$;
drop trigger if exists seed_teacher_lesson_times_trigger on public.profiles;
create trigger seed_teacher_lesson_times_trigger after insert or update of role,active
on public.profiles for each row execute function public.seed_teacher_lesson_times();

-- ---------- GOOGLE SHEETS ----------
create table if not exists public.google_sheets_config (
    id boolean primary key default true check (id),
    enabled boolean not null default false,
    spreadsheet_id text not null default '',
    sheet_name text not null default 'Vertinimai',
    sheet_per_teacher boolean not null default false,
    updated_at timestamptz not null default now()
);
insert into public.google_sheets_config(id) values(true) on conflict(id) do nothing;
create table if not exists public.google_sheets_exports (
    rating_id uuid primary key references public.ratings(id) on delete cascade,
    status text not null check(status in ('pending','exported','failed')),
    updated_at timestamptz not null default now()
);

-- ---------- ATSILIEPIMAI ----------
create table if not exists public.feedback (
    id uuid primary key default gen_random_uuid(),
    teacher_id uuid not null references public.profiles(id) on delete cascade,
    message text not null,
    read boolean not null default false,
    created_at timestamptz not null default now()
);
create index if not exists feedback_created_at_idx on public.feedback(created_at desc);

-- ---------- SAUGIOS PAGALBINES FUNKCIJOS ----------
create or replace function public.is_admin()
returns boolean language sql stable security definer set search_path = public as $$
    select exists(select 1 from public.profiles p where p.id=auth.uid() and p.role='admin' and p.active);
$$;
create or replace function public.is_active_user()
returns boolean language sql stable security definer set search_path = public as $$
    select exists(select 1 from public.profiles p where p.id=auth.uid() and p.active and p.role in ('teacher','admin'));
$$;

create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    insert into public.profiles(id,email,full_name)
    values(new.id,coalesce(new.email,''),coalesce(new.raw_user_meta_data->>'full_name',''))
    on conflict(id) do nothing;
    return new;
end;
$$;
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
for each row execute function public.handle_new_user();
insert into public.profiles(id,email,full_name)
select u.id,coalesce(u.email,''),coalesce(u.raw_user_meta_data->>'full_name','') from auth.users u
on conflict(id) do nothing;

create or replace function public.redeem_invite(p_code text)
returns json language plpgsql security definer set search_path = public as $$
declare v_invite public.invites; v_email text;
begin
    if auth.uid() is null then return json_build_object('ok',false,'error','Reikia prisijungti'); end if;
    select email into v_email from auth.users where id=auth.uid();
    select * into v_invite from public.invites where upper(code)=upper(trim(p_code)) for update;
    if v_invite.id is null then return json_build_object('ok',false,'error','Kvietimo kodas nerastas'); end if;
    if v_invite.redeemed_at is not null then return json_build_object('ok',false,'error','Šis kvietimas jau panaudotas'); end if;
    if v_invite.expires_at < now() then return json_build_object('ok',false,'error','Kvietimo galiojimas baigėsi'); end if;
    if lower(v_invite.email) <> lower(coalesce(v_email,'')) then return json_build_object('ok',false,'error','Kvietimas išduotas kitam el. paštui'); end if;
    update public.invites set redeemed_at=now(),redeemed_by=auth.uid() where id=v_invite.id;
    update public.profiles set role='teacher',active=true where id=auth.uid();
    return json_build_object('ok',true,'error',null);
end;
$$;
create or replace function public.set_my_name(p_name text)
returns void language sql security definer set search_path = public as $$
    update public.profiles set full_name=left(coalesce(p_name,''),80) where id=auth.uid();
$$;

-- Export claims are service-only; this function validates caller identity and rating ownership.
create or replace function public.claim_google_sheets_export(p_rating_id uuid)
returns boolean language plpgsql security definer set search_path = public as $$
declare claimed_id uuid;
begin
    if not public.is_active_user() or not exists(select 1 from public.ratings r
        where r.id=p_rating_id and (r.teacher_id=auth.uid() or public.is_admin())) then return false; end if;
    insert into public.google_sheets_exports(rating_id,status,updated_at) values(p_rating_id,'pending',now())
    on conflict(rating_id) do update set status='pending',updated_at=now()
    where public.google_sheets_exports.status='failed'
       or (public.google_sheets_exports.status='pending' and public.google_sheets_exports.updated_at<now()-interval '5 minutes')
    returning rating_id into claimed_id;
    return claimed_id is not null;
end;
$$;
revoke all on function public.claim_google_sheets_export(uuid) from public, anon;
grant execute on function public.claim_google_sheets_export(uuid) to authenticated;

-- ---------- RLS / PRIVILEGES ----------
alter table public.profiles enable row level security;
alter table public.classes enable row level security;
alter table public.invites enable row level security;
alter table public.ratings enable row level security;
alter table public.teacher_classes enable row level security;
alter table public.lesson_times enable row level security;
alter table public.google_sheets_config enable row level security;
alter table public.google_sheets_exports enable row level security;
alter table public.feedback enable row level security;

drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles for select to authenticated using(id=auth.uid() or public.is_admin());
drop policy if exists profiles_admin_update on public.profiles;
create policy profiles_admin_update on public.profiles for update to authenticated using(public.is_admin()) with check(public.is_admin());
drop policy if exists classes_select on public.classes;
create policy classes_select on public.classes for select to authenticated using(public.is_active_user());
drop policy if exists classes_admin_write on public.classes;
create policy classes_admin_write on public.classes for all to authenticated using(public.is_admin()) with check(public.is_admin());
drop policy if exists invites_admin_only on public.invites;
create policy invites_admin_only on public.invites for all to authenticated using(public.is_admin()) with check(public.is_admin());
drop policy if exists ratings_select on public.ratings;
create policy ratings_select on public.ratings for select to authenticated using(teacher_id=auth.uid() or public.is_admin());
drop policy if exists ratings_insert on public.ratings;
create policy ratings_insert on public.ratings for insert to authenticated with check(public.is_admin() or (teacher_id=auth.uid() and public.is_active_user()));
drop policy if exists ratings_admin_delete on public.ratings;
create policy ratings_admin_delete on public.ratings for delete to authenticated using(public.is_admin());
drop policy if exists ratings_update on public.ratings;
create policy ratings_update on public.ratings for update to authenticated using(teacher_id=auth.uid()) with check(teacher_id=auth.uid());
drop policy if exists teacher_classes_select on public.teacher_classes;
create policy teacher_classes_select on public.teacher_classes for select to authenticated using(teacher_id=auth.uid() or public.is_admin());
drop policy if exists teacher_classes_admin_write on public.teacher_classes;
create policy teacher_classes_admin_write on public.teacher_classes for all to authenticated using(public.is_admin()) with check(public.is_admin());
drop policy if exists lesson_times_select on public.lesson_times;
create policy lesson_times_select on public.lesson_times for select to authenticated using(teacher_id=auth.uid() or public.is_admin());
drop policy if exists lesson_times_admin_write on public.lesson_times;
create policy lesson_times_admin_write on public.lesson_times for all to authenticated using(public.is_admin()) with check(public.is_admin());
drop policy if exists google_sheets_config_admin on public.google_sheets_config;
create policy google_sheets_config_admin on public.google_sheets_config for all to authenticated using(public.is_admin()) with check(public.is_admin());
drop policy if exists feedback_insert on public.feedback;
create policy feedback_insert on public.feedback for insert to authenticated with check(public.is_active_user() and teacher_id=auth.uid());
drop policy if exists feedback_select on public.feedback;
create policy feedback_select on public.feedback for select to authenticated using(public.is_admin() or teacher_id=auth.uid());
drop policy if exists feedback_admin_write on public.feedback;
create policy feedback_admin_write on public.feedback for update to authenticated using(public.is_admin()) with check(public.is_admin());
drop policy if exists feedback_admin_delete on public.feedback;
create policy feedback_admin_delete on public.feedback for delete to authenticated using(public.is_admin());

grant select, update on public.profiles to authenticated;
grant select, insert, update, delete on public.classes, public.invites, public.teacher_classes to authenticated;
grant select, insert, update, delete on public.ratings, public.lesson_times to authenticated;
grant select, insert, update on public.google_sheets_config to authenticated;
grant select, insert, update, delete on public.feedback to authenticated;
revoke all on public.google_sheets_exports from anon, authenticated;
grant all on public.google_sheets_exports to service_role;
grant select on public.profiles, public.classes to service_role;
grant select, insert, update on public.ratings to service_role;
