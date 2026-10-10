-- Pedido por chat: "sub-cortes" que la balanza reconoce por separado (ej. "Bife de
-- chorizo", que sale de "Bife angosto") pero que no se despostan por su cuenta. Se venden
-- con su propio precio (para que la caja diaria quede bien), pero el stock que se
-- descuenta es el de otro corte — el que realmente se cargó en el despostado. Un solo
-- nivel (el corte de destino no puede a su vez redirigir a otro): se valida en
-- CorteService, no acá, para dar un mensaje de error claro en vez de un fallo de FK.
alter table cortes add column descuenta_stock_de_corte_id uuid references cortes(id);

-- Reemplaza la vista de V16: misma estructura de columnas (corte_id, entrado_kg,
-- vendido_kg, stock_kg), pero el vendido_kg de un corte ahora suma también las ventas de
-- cualquier corte que lo tenga como destino. Los cortes que redirigen (tienen
-- descuenta_stock_de_corte_id seteado) quedan afuera del resultado — nunca tienen stock
-- propio, mostrarlos en 0 sería confuso en vez de simplemente no listarlos.
create or replace view stock_por_corte
with (security_invoker = true) as
select
  c.id as corte_id,
  coalesce(d.entrado_kg, 0) as entrado_kg,
  coalesce(v.vendido_kg, 0) + coalesce(v_redirigido.vendido_kg, 0) as vendido_kg,
  coalesce(d.entrado_kg, 0) - (coalesce(v.vendido_kg, 0) + coalesce(v_redirigido.vendido_kg, 0)) as stock_kg
from cortes c
left join (
  select corte_id, sum(kg) as entrado_kg from despostado group by corte_id
) d on d.corte_id = c.id
left join (
  select corte_id, sum(kg) as vendido_kg from ventas where anulada = false group by corte_id
) v on v.corte_id = c.id
left join (
  select co.descuenta_stock_de_corte_id as corte_id, sum(ve.kg) as vendido_kg
  from ventas ve
  join cortes co on co.id = ve.corte_id
  where ve.anulada = false and co.descuenta_stock_de_corte_id is not null
  group by co.descuenta_stock_de_corte_id
) v_redirigido on v_redirigido.corte_id = c.id
where c.descuenta_stock_de_corte_id is null;
