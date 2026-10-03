-- Hallazgo de seguridad en V10__multi_negocio.sql: las políticas "*_dueno_todo" (cortes,
-- medias_reses, despostado, perdidas) acotaban por negocio (`dueno_id`/`creado_por` =
-- `mi_negocio_id()`) pero no por rol. Un empleado comparte el mismo `mi_negocio_id()` que
-- su dueño (es la gracia de la función), así que esas políticas "for all" también lo
-- dejaban pasar a INSERT/UPDATE/DELETE — no solo a los SELECT que sí le corresponden por
-- su propia política de empleado. No se detectó en los tests existentes porque
-- "empleado_noPuedeCargarNiLeerEntradas" usa un desconocido sin ningún perfil
-- (mi_negocio_id() = null, nunca matchea), no un empleado real del mismo negocio.
-- Se agrega `is_dueno()` (V9) a las cuatro políticas, además del chequeo de negocio.
drop policy "cortes_dueno_todo" on cortes;
create policy "cortes_dueno_todo" on cortes
  for all
  using (is_dueno() and dueno_id = mi_negocio_id())
  with check (is_dueno() and dueno_id = mi_negocio_id());

drop policy "medias_reses_dueno_todo" on medias_reses;
create policy "medias_reses_dueno_todo" on medias_reses
  for all
  using (is_dueno() and creado_por = mi_negocio_id())
  with check (is_dueno() and creado_por = mi_negocio_id());

drop policy "despostado_dueno_todo" on despostado;
create policy "despostado_dueno_todo" on despostado
  for all
  using (is_dueno() and exists (
    select 1 from medias_reses m where m.id = despostado.media_res_id and m.creado_por = mi_negocio_id()
  ))
  with check (is_dueno() and exists (
    select 1 from medias_reses m where m.id = despostado.media_res_id and m.creado_por = mi_negocio_id()
  ));

drop policy "perdidas_dueno_todo" on perdidas;
create policy "perdidas_dueno_todo" on perdidas
  for all
  using (is_dueno() and exists (
    select 1 from medias_reses m where m.id = perdidas.media_res_id and m.creado_por = mi_negocio_id()
  ))
  with check (is_dueno() and exists (
    select 1 from medias_reses m where m.id = perdidas.media_res_id and m.creado_por = mi_negocio_id()
  ));
