create table ventas (
  id uuid primary key default gen_random_uuid(),
  fecha_hora timestamptz not null default now(),
  corte_id uuid not null references cortes (id),
  kg numeric(8, 3) not null check (kg > 0),
  codigo_leido text not null,
  -- Lo genera el frontend al capturar el escaneo (no el backend): detecta un "Enter"
  -- duplicado del lector sin tener que saber primero si el código es válido, y sirve
  -- igual cuando llegue la cola offline de Fase 3 sin cambiar el contrato.
  id_cliente_local uuid not null unique,
  anulada boolean not null default false,
  -- Quién escaneó (dueño o empleado) vs. a qué negocio pertenece la venta: a diferencia
  -- de medias_reses (solo el dueño carga, los dos valores siempre coinciden), acá un
  -- empleado puede ser quien escanea sin ser el dueño del negocio.
  usuario_id uuid not null references auth.users (id),
  dueno_id uuid not null references auth.users (id)
);

alter table ventas enable row level security;

-- Mismo criterio que V11__fix_dueno_todo_exige_rol.sql: is_dueno() además del chequeo de
-- negocio, para que un empleado del mismo negocio no entre por esta política "for all".
create policy "ventas_dueno_todo" on ventas
  for all
  using (is_dueno() and dueno_id = mi_negocio_id())
  with check (is_dueno() and dueno_id = mi_negocio_id());

create policy "ventas_empleado_select" on ventas
  for select
  using (dueno_id = mi_negocio_id());

create policy "ventas_empleado_insert" on ventas
  for insert
  with check (dueno_id = mi_negocio_id() and usuario_id = auth.uid());
