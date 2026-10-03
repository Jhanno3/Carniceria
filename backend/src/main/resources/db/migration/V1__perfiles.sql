create extension if not exists pgcrypto;

create table perfiles (
  id uuid primary key references auth.users (id) on delete cascade,
  nombre text,
  rol text not null check (rol in ('dueno', 'empleado'))
);

alter table perfiles enable row level security;

-- Cada usuario lee únicamente su propia fila (para saber su propio rol en el frontend).
create policy "perfiles_select_propio" on perfiles
  for select
  using (id = auth.uid());

-- Helper de autorización reutilizado por el resto de las políticas RLS de esta fase.
-- security definer: necesita leer perfiles sin quedar atado a la política de arriba
-- (que solo deja ver la fila propia), pero solo responde si YO soy dueño, nunca expone
-- la fila de otro usuario.
create or replace function is_dueno()
returns boolean
language sql
security definer
set search_path = public
stable
as $$
  select exists (
    select 1 from perfiles where id = auth.uid() and rol = 'dueno'
  );
$$;
