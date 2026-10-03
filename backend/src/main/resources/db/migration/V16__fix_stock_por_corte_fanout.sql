-- Hallazgo: V15 hacía left join a despostado Y a ventas en la misma consulta, ambos sobre
-- cortes.id, sin relación entre sí — un corte con N filas de despostado y M de ventas
-- generaba N×M filas combinadas antes del group by, así que sum(d.kg) contaba cada fila
-- de despostado una vez POR CADA venta de ese corte (y viceversa). Se corrige agregando
-- cada lado por separado ANTES de unirlo a cortes, sin productos cartesianos.
create or replace view stock_por_corte
with (security_invoker = true) as
select
  c.id as corte_id,
  coalesce(d.entrado_kg, 0) as entrado_kg,
  coalesce(v.vendido_kg, 0) as vendido_kg,
  coalesce(d.entrado_kg, 0) - coalesce(v.vendido_kg, 0) as stock_kg
from cortes c
left join (
  select corte_id, sum(kg) as entrado_kg from despostado group by corte_id
) d on d.corte_id = c.id
left join (
  select corte_id, sum(kg) as vendido_kg from ventas where anulada = false group by corte_id
) v on v.corte_id = c.id;
