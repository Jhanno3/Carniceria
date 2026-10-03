-- Pivot a multi-negocio: hasta acá "dueno" era un único nivel con acceso total sobre
-- TODOS los datos (pensado para una sola carnicería). De acá en más cada "dueno" es un
-- negocio/tenant aislado, con su propio catálogo de cortes y sus propias medias reses;
-- cada "empleado" queda vinculado a un único dueño (el que lo invitó). "admin" (un solo
-- usuario, V9__rol_admin.sql) no es dueño de ningún negocio: solo gestiona qué cuentas de
-- dueño existen, nunca ve los datos operativos de ninguna (decisión explícita del dueño
-- real de la plataforma).

alter table perfiles add column dueno_id uuid references auth.users(id);

-- Para un "dueno", su propio negocio es él mismo: tenant_id = su propio id. Simplifica
-- toda política de ahora en más a "¿la fila es de mi negocio?" = "dueno_id de la fila
-- es igual al dueno_id de mi perfil". No hay ninguna fila con rol='dueno' todavía (las
-- dos existentes pasaron a 'admin' en V9), así que este UPDATE es un no-op en la
-- práctica — queda igual para cuando exista el primer dueño real.
update perfiles set dueno_id = id where rol = 'dueno';

create or replace function mi_negocio_id()
returns uuid
language sql
security definer
set search_path = public
stable
as $$
  select dueno_id from perfiles where id = auth.uid();
$$;

-- Valida un código de invitación (el propio id del dueño, ver PerfilService) sin
-- exponerle el resto de su fila a alguien que todavía no tiene ningún perfil.
create or replace function es_dueno_valido(candidato uuid)
returns boolean
language sql
security definer
set search_path = public
stable
as $$
  select exists (
    select 1 from perfiles where id = candidato and rol = 'dueno' and estado = 'aprobado'
  );
$$;

-- Los 17 cortes sembrados en V3/V8 y la media res de prueba que los usaba eran datos de
-- cuando esto era de un solo negocio (ninguna cuenta real de "dueno" existe todavía:
-- Facundo pasó a "admin" en V9). Se borran: de acá en más, cada "dueno" nuevo se siembra
-- su propio catálogo al ser aprobado (ver PerfilService.actualizar).
delete from perdidas;
delete from despostado;
delete from medias_reses;
delete from cortes;

alter table cortes drop constraint cortes_plu_key;
alter table cortes add column dueno_id uuid not null references auth.users(id);
alter table cortes add constraint cortes_dueno_id_plu_key unique (dueno_id, plu);

-- medias_reses NO suma una columna dueno_id propia: solo el dueño carga medias reses
-- (nunca un empleado, ver FR-101 y el test empleado_noPuedeCargarNiLeerEntradas), así
-- que creado_por YA ES el tenant — agregar otra columna sería duplicar el mismo dato
-- (Principio VII). Si alguna fase futura permite que alguien más cargue en nombre de un
-- dueño, ahí sí hace falta separarlos.

-- Reemplaza is_dueno() por el chequeo de tenant en las 4 tablas de negocio. is_dueno()
-- (V9) sigue valiendo para todo lo que no es por-negocio (nada queda, de hecho, salvo
-- que una fase futura agregue otra tabla global).
drop policy "cortes_dueno_todo" on cortes;
create policy "cortes_dueno_todo" on cortes
  for all
  using (dueno_id = mi_negocio_id())
  with check (dueno_id = mi_negocio_id());

-- También por-negocio: un empleado de un dueño no debe ver el catálogo de otro.
drop policy "cortes_empleado_select_activos" on cortes;
create policy "cortes_empleado_select_activos" on cortes
  for select
  using (activo = true and dueno_id = mi_negocio_id());

drop policy "medias_reses_dueno_todo" on medias_reses;
create policy "medias_reses_dueno_todo" on medias_reses
  for all
  using (creado_por = mi_negocio_id())
  with check (creado_por = mi_negocio_id());

drop policy "despostado_dueno_todo" on despostado;
create policy "despostado_dueno_todo" on despostado
  for all
  using (exists (
    select 1 from medias_reses m where m.id = despostado.media_res_id and m.creado_por = mi_negocio_id()
  ))
  with check (exists (
    select 1 from medias_reses m where m.id = despostado.media_res_id and m.creado_por = mi_negocio_id()
  ));

drop policy "perdidas_dueno_todo" on perdidas;
create policy "perdidas_dueno_todo" on perdidas
  for all
  using (exists (
    select 1 from medias_reses m where m.id = perdidas.media_res_id and m.creado_por = mi_negocio_id()
  ))
  with check (exists (
    select 1 from medias_reses m where m.id = perdidas.media_res_id and m.creado_por = mi_negocio_id()
  ));
