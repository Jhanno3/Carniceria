create table medias_reses (
  id uuid primary key default gen_random_uuid(),
  fecha date not null default current_date,
  proveedor text,
  peso_kg numeric(8, 3) not null check (peso_kg > 0),
  precio_kg numeric(12, 2) check (precio_kg is null or precio_kg > 0),
  creado_por uuid not null references auth.users (id),
  creado_en timestamptz not null default now()
);

alter table medias_reses enable row level security;

-- Solo el dueño: ni siquiera existe una política de lectura para empleado en esta tabla
-- (nunca ve precio_kg ni ninguna otra columna — constitution.md, Principio II).
create policy "medias_reses_dueno_todo" on medias_reses
  for all
  using (is_dueno())
  with check (is_dueno());
