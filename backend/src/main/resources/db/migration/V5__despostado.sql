create table despostado (
  id uuid primary key default gen_random_uuid(),
  media_res_id uuid not null references medias_reses (id) on delete cascade,
  corte_id uuid not null references cortes (id),
  kg numeric(8, 3) not null check (kg > 0),
  unique (media_res_id, corte_id)
);

alter table despostado enable row level security;

-- Solo dueño en esta fase. La política de lectura para el cálculo de stock (Fase 2)
-- se agrega en la migración de esa fase, no antes (constitution.md, Principio VII).
create policy "despostado_dueno_todo" on despostado
  for all
  using (is_dueno())
  with check (is_dueno());
