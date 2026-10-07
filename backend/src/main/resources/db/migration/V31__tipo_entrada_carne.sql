-- Fase 7 (ajuste pedido por chat): tercer valor de tipo_entrada para que el modal
-- "Añadir stock" pueda etiquetar las entradas de la nueva categoría "Carne".
do $$
declare
  nombre_constraint text;
begin
  select conname into nombre_constraint
  from pg_constraint
  where conrelid = 'medias_reses'::regclass
    and contype = 'c'
    and pg_get_constraintdef(oid) ilike '%tipo_entrada%mediares%';
  if nombre_constraint is not null then
    execute format('alter table medias_reses drop constraint %I', nombre_constraint);
  end if;
end $$;

alter table medias_reses add constraint medias_reses_tipo_entrada_check
  check (tipo_entrada in ('MediaRes', 'Delantero', 'Pecho', 'Parrillero', 'AsadoCompleto',
    'Mocho', 'Rueda', 'AchurasEmbutidos', 'Cerdo', 'Carne'));
