create table perdidas (
  id uuid primary key default gen_random_uuid(),
  media_res_id uuid not null references medias_reses (id) on delete cascade,
  tipo text not null check (tipo in ('hueso', 'grasa', 'merma')),
  kg numeric(8, 3) not null check (kg > 0),
  unique (media_res_id, tipo)
);

alter table perdidas enable row level security;

create policy "perdidas_dueno_todo" on perdidas
  for all
  using (is_dueno())
  with check (is_dueno());
