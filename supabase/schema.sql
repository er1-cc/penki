-- ============================================================
--  penki – klasės elgesio (mikroklimato) vertinimo programa
--  Paleisk šį failą Supabase → SQL Editor → Run
-- ============================================================

create extension if not exists pgcrypto;

-- ---------- 1. PROFILIAI ----------
create table if not exists public.profiles (
    id          uuid primary key references auth.users (id) on delete cascade,
    email       text not null default '',
    full_name   text not null default '',
    role        text not null default 'pending' check (role in ('pending', 'teacher', 'admin')),
    active      boolean not null default false,
    created_at  timestamptz not null default now()
);

-- ---------- 2. KLASĖS ----------
create table if not exists public.classes (
    id          uuid primary key default gen_random_uuid(),
    name        text not null unique,
    active      boolean not null default true,
    created_at  timestamptz not null default now()
);

-- ---------- 3. KVIETIMAI ----------
create table if not exists public.invites (
    id          uuid primary key default gen_random_uuid(),
    email       text not null,
    code        text not null unique,
    created_by  uuid references public.profiles (id) on delete set null,
    created_at  timestamptz not null default now(),
    expires_at  timestamptz not null default now() + interval '14 days',
    redeemed_at timestamptz,
    redeemed_by uuid references public.profiles (id) on delete set null
);

-- ---------- 4. VERTINIMAI ----------
create table if not exists public.ratings (
    id          uuid primary key default gen_random_uuid(),
    teacher_id  uuid not null references public.profiles (id) on delete cascade,
    class_id    uuid not null references public.classes (id) on delete cascade,
    lesson_no   int not null check (lesson_no between 1 and 10),
    score       int not null check (score between 1 and 5),
    comment     text not null default '',
    rated_on    date not null default (now() at time zone 'Europe/Vilnius')::date,
    created_at  timestamptz not null default now()
);

create index if not exists ratings_rated_on_idx on public.ratings (rated_on desc);
create index if not exists ratings_teacher_idx on public.ratings (teacher_id);
create unique index if not exists ratings_unique_lesson_idx
    on public.ratings (teacher_id, class_id, rated_on, lesson_no);

-- ---------- 5. MOKYTOJŲ KLASIŲ PRISKYRIMAS ----------
-- Jei mokytojas turi bent vieną priskyrimą, jam rodomos tik tos klasės.
-- Jei priskyrimų nėra – rodomos visos aktyvios klasės.
create table if not exists public.teacher_classes (
    teacher_id  uuid not null references public.profiles (id) on delete cascade,
    class_id    uuid not null references public.classes (id) on delete cascade,
    created_at  timestamptz not null default now(),
    primary key (teacher_id, class_id)
);

-- ---------- 6. PAMOKŲ LAIKAI ----------
-- Administratorius gali keisti, kada baigiasi kiekviena pamoka.
-- Laikų sąrašas nulemia ir priminimų laikus telefone.
create table if not exists public.lesson_times (
    lesson_no   int primary key check (lesson_no between 1 and 10),
    ends_at     time not null,
    updated_at  timestamptz not null default now()
);

insert into public.lesson_times (lesson_no, ends_at) values
    (1, '08:45'), (2, '09:40'), (3, '10:35'), (4, '11:30'),
    (5, '12:25'), (6, '13:20'), (7, '14:15')
on conflict (lesson_no) do nothing;

-- ---------- 7. PAGALBINĖS FUNKCIJOS ----------
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.profiles p
        where p.id = auth.uid() and p.role = 'admin' and p.active
    );
$$;

create or replace function public.is_active_user()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select exists (
        select 1 from public.profiles p
        where p.id = auth.uid() and p.active and p.role in ('teacher', 'admin')
    );
$$;

-- Naujas auth vartotojas automatiškai gauna profilį (neaktyvų, kol neatpažintas kvietimas)
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.profiles (id, email, full_name)
    values (
        new.id,
        coalesce(new.email, ''),
        coalesce(new.raw_user_meta_data ->> 'full_name', '')
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- Profilis sukuriamas ir tada, jei vartotojas jau egzistavo prieš įdiegiant šią schemą
insert into public.profiles (id, email, full_name)
select u.id, coalesce(u.email, ''), coalesce(u.raw_user_meta_data ->> 'full_name', '')
from auth.users u
on conflict (id) do nothing;

-- Kvietimo aktyvavimas: kodas + el. paštas turi sutapti, kodas vienkartinis
create or replace function public.redeem_invite(p_code text)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
    v_invite public.invites;
    v_email  text;
begin
    if auth.uid() is null then
        return json_build_object('ok', false, 'error', 'Reikia prisijungti');
    end if;

    select email into v_email from auth.users where id = auth.uid();

    select * into v_invite
    from public.invites
    where upper(code) = upper(trim(p_code))
    for update;

    if v_invite.id is null then
        return json_build_object('ok', false, 'error', 'Kvietimo kodas nerastas');
    end if;
    if v_invite.redeemed_at is not null then
        return json_build_object('ok', false, 'error', 'Šis kvietimas jau panaudotas');
    end if;
    if v_invite.expires_at < now() then
        return json_build_object('ok', false, 'error', 'Kvietimo galiojimas baigėsi');
    end if;
    if lower(v_invite.email) <> lower(coalesce(v_email, '')) then
        return json_build_object('ok', false, 'error', 'Kvietimas išduotas kitam el. paštui');
    end if;

    update public.invites
    set redeemed_at = now(), redeemed_by = auth.uid()
    where id = v_invite.id;

    update public.profiles
    set role = 'teacher', active = true
    where id = auth.uid();

    return json_build_object('ok', true, 'error', null);
end;
$$;

-- Vardas keičiamas tik per šią funkciją (kad vartotojas negalėtų keisti rolės)
create or replace function public.set_my_name(p_name text)
returns void
language sql
security definer
set search_path = public
as $$
    update public.profiles set full_name = left(coalesce(p_name, ''), 80) where id = auth.uid();
$$;

-- ---------- 8. RLS ----------
alter table public.profiles        enable row level security;
alter table public.classes         enable row level security;
alter table public.invites         enable row level security;
alter table public.ratings         enable row level security;
alter table public.teacher_classes enable row level security;
alter table public.lesson_times    enable row level security;

drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles
    for select to authenticated
    using (id = auth.uid() or public.is_admin());

drop policy if exists profiles_admin_update on public.profiles;
create policy profiles_admin_update on public.profiles
    for update to authenticated
    using (public.is_admin())
    with check (public.is_admin());

drop policy if exists classes_select on public.classes;
create policy classes_select on public.classes
    for select to authenticated
    using (public.is_active_user());

drop policy if exists classes_admin_write on public.classes;
create policy classes_admin_write on public.classes
    for all to authenticated
    using (public.is_admin())
    with check (public.is_admin());

drop policy if exists invites_admin_only on public.invites;
create policy invites_admin_only on public.invites
    for all to authenticated
    using (public.is_admin())
    with check (public.is_admin());

drop policy if exists ratings_select on public.ratings;
create policy ratings_select on public.ratings
    for select to authenticated
    using (teacher_id = auth.uid() or public.is_admin());

drop policy if exists ratings_insert on public.ratings;
create policy ratings_insert on public.ratings
    for insert to authenticated
    with check (public.is_admin() or (teacher_id = auth.uid() and public.is_active_user()));

drop policy if exists ratings_admin_delete on public.ratings;
create policy ratings_admin_delete on public.ratings
    for delete to authenticated
    using (public.is_admin());

drop policy if exists teacher_classes_select on public.teacher_classes;
create policy teacher_classes_select on public.teacher_classes
    for select to authenticated
    using (teacher_id = auth.uid() or public.is_admin());

drop policy if exists teacher_classes_admin_write on public.teacher_classes;
create policy teacher_classes_admin_write on public.teacher_classes
    for all to authenticated
    using (public.is_admin())
    with check (public.is_admin());

drop policy if exists lesson_times_select on public.lesson_times;
create policy lesson_times_select on public.lesson_times
    for select to authenticated
    using (public.is_active_user());

drop policy if exists lesson_times_admin_write on public.lesson_times;
create policy lesson_times_admin_write on public.lesson_times
    for all to authenticated
    using (public.is_admin())
    with check (public.is_admin());

drop policy if exists ratings_update on public.ratings;
create policy ratings_update on public.ratings
    for update to authenticated
    using (teacher_id = auth.uid())
    with check (teacher_id = auth.uid());

-- ---------- 9. PIRMASIS ADMINAS ----------
-- 1) Supabase → Authentication → Users → Add user (el. paštas + slaptažodis)
-- 2) Tada paleisk (pakeisk el. paštą):
--
-- update public.profiles set role = 'admin', active = true
-- where email = 'admin@mokykla.lt';
--
-- Po to adminas programėlėje kuria kvietimus mokytojams.
