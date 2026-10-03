-- Sin RLS propia ni dueno_id: security_invoker hace que las políticas de cortes/despostado/
-- ventas se evalúen con el rol Y el negocio de quien consulta, no con el del dueño de la
-- vista (si no, cualquier negocio vería el stock de cualquier otro — mismo tipo de
-- hallazgo que V11__fix_dueno_todo_exige_rol.sql).
create view stock_por_corte
with (security_invoker = true) as
select
  c.id as corte_id,
  coalesce(sum(d.kg), 0) as entrado_kg,
  coalesce(sum(v.kg) filter (where v.anulada = false), 0) as vendido_kg,
  coalesce(sum(d.kg), 0) - coalesce(sum(v.kg) filter (where v.anulada = false), 0) as stock_kg
from cortes c
left join despostado d on d.corte_id = c.id
left join ventas v on v.corte_id = c.id
group by c.id;
