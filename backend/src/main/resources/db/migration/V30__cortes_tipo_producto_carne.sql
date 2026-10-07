-- Fase 7 (ajuste pedido por chat tras probar el modal): tercera categoría de compra
-- directa, "Carne" — Rabo y Carne picada/recortes no se despostan de una media res
-- tampoco, pero son carne vacuna (no achuras/embutidos ni cerdo), así que van aparte.
-- Importante: NO filtrar solo por "tipo_producto" + "vacuno" acá — el constraint de
-- `cuarto` (V27) también menciona ambas palabras (su CHECK compara contra
-- tipo_producto = 'Vacuno'), y un SELECT ... INTO con más de una fila se queda con
-- cualquiera de las dos sin avisar. 'AchurasEmbutidos'/'Cerdo' son exclusivos del check
-- de tipo_producto — no aparecen en el de cuarto.
do $$
declare
  nombre_constraint text;
begin
  select conname into nombre_constraint
  from pg_constraint
  where conrelid = 'cortes'::regclass
    and contype = 'c'
    and pg_get_constraintdef(oid) ilike '%achurasembutidos%cerdo%';
  if nombre_constraint is not null then
    execute format('alter table cortes drop constraint %I', nombre_constraint);
  end if;
end $$;

alter table cortes add constraint cortes_tipo_producto_check
  check (tipo_producto in ('Vacuno', 'AchurasEmbutidos', 'Cerdo', 'Carne'));

-- Reclasifica los dos cortes que ya existían con otro tipo_producto. Se matchea por
-- nombre (no por plu, que es por-dueño y editable) y se acota al tipo_producto de origen
-- esperado, para no tocar un corte que el dueño ya haya reclasificado/renombrado a mano.
update cortes set tipo_producto = 'Carne', cuarto = null
  where nombre = 'Carne picada / recortes' and tipo_producto = 'Vacuno';

update cortes set tipo_producto = 'Carne'
  where nombre = 'Rabo' and tipo_producto = 'AchurasEmbutidos';
