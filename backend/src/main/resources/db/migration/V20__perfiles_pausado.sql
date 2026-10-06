-- FR-406 (US-4.4): el admin puede pausar el acceso de cualquier cuenta sin borrarla.
-- Se agrega un cuarto valor al estado existente en vez de una columna booleana aparte
-- (plan-fase4.md, 3.7) — reutiliza PUT /perfiles/{id} tal cual.
--
-- El nombre de la constraint original (V7__perfiles_estado.sql, "add column ... check
-- (...)" inline) no se registró a mano en ningún lado de este repo; se busca en
-- pg_constraint en vez de asumir el nombre que Postgres le puso solo, para no romper la
-- migración si el nombre real no fuera el esperado.
do $$
declare
  nombre_constraint text;
begin
  select conname into nombre_constraint
  from pg_constraint
  where conrelid = 'perfiles'::regclass
    and contype = 'c'
    and pg_get_constraintdef(oid) ilike '%estado%pendiente%aprobado%rechazado%';
  if nombre_constraint is not null then
    execute format('alter table perfiles drop constraint %I', nombre_constraint);
  end if;
end $$;

alter table perfiles add constraint perfiles_estado_check
  check (estado in ('pendiente', 'aprobado', 'rechazado', 'pausado'));

-- mi_negocio_id() (V10__multi_negocio.sql) nunca chequeó estado: is_dueno()/is_admin() ya
-- lo hacían (V7/V9) para sus propios usos, pero esta función la usan TODAS las políticas
-- por-negocio (cortes, medias_reses, despostado, perdidas, ventas, config_etiqueta) —
-- sin este cambio, pausar a un EMPLEADO no le sacaría ningún acceso real: su dueno_id
-- seguiría resolviendo su negocio igual. Con este cambio, cualquier cuenta no aprobada
-- (pendiente, rechazada o pausada) deja de pertenecer a ningún negocio para todas esas
-- políticas a la vez (plan-fase4.md, 3.7).
create or replace function mi_negocio_id()
returns uuid
language sql
security definer
set search_path = public
stable
as $$
  select dueno_id from perfiles where id = auth.uid() and estado = 'aprobado';
$$;
