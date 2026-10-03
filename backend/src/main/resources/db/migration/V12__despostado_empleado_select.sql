-- Fase 2: el mostrador necesita leer despostado para que la vista de stock le dé números
-- correctos a un empleado (stock_por_corte, V15). Política anticipada desde V2__cortes.sql,
-- acotada al propio negocio igual que la de "dueno" (V10/V11): un empleado nunca escribe,
-- solo lee.
create policy "despostado_empleado_select" on despostado
  for select
  using (exists (
    select 1 from medias_reses m where m.id = despostado.media_res_id and m.creado_por = mi_negocio_id()
  ));
