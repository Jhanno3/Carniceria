-- Una fila por negocio (no una tabla singleton global): cada carnicería puede tener una
-- balanza configurada distinto. La PK es directamente el dueño, no un id propio — "la"
-- fila de configuración de ESE negocio.
create table config_etiqueta (
  dueno_id uuid primary key references auth.users (id),
  prefijo_desde integer not null check (prefijo_desde between 20 and 29),
  prefijo_hasta integer not null check (prefijo_hasta between 20 and 29),
  inicio_plu integer not null check (inicio_plu >= 0),
  largo_plu integer not null check (largo_plu > 0),
  inicio_valor integer not null check (inicio_valor >= 0),
  largo_valor integer not null check (largo_valor > 0),
  tipo_valor text not null check (tipo_valor in ('peso', 'importe')),
  decimales integer not null check (decimales >= 0)
);

alter table config_etiqueta enable row level security;

create policy "config_etiqueta_dueno_todo" on config_etiqueta
  for all
  using (is_dueno() and dueno_id = mi_negocio_id())
  with check (is_dueno() and dueno_id = mi_negocio_id());
