create table cortes (
  id uuid primary key default gen_random_uuid(),
  nombre text not null,
  plu integer not null unique,
  cuarto text not null check (cuarto in ('Delantero', 'Trasero', 'Ambos')),
  zona_mapa text,
  activo boolean not null default true
);

alter table cortes enable row level security;

create policy "cortes_dueno_todo" on cortes
  for all
  using (is_dueno())
  with check (is_dueno());

-- El mostrador (Fase 2) necesita leer los cortes activos para mostrar stock.
-- Se define ya (especificacion-carniceria.md, sección 8.2) aunque esta fase no la use todavía.
create policy "cortes_empleado_select_activos" on cortes
  for select
  using (activo = true);
