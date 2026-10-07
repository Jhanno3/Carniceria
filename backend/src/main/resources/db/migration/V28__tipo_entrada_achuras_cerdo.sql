-- Fase 7: dos valores nuevos de tipo_entrada para las entradas que arma el modal
-- "Añadir stock" de Control diario (Achuras/Embutidos y Cerdo comprados ya terminados,
-- no se despostan de una media res). No se ofrecen en el selector "Corte" de Despostado
-- (Fase 6) — eso sigue con sus 7 valores de siempre.
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
    'Mocho', 'Rueda', 'AchurasEmbutidos', 'Cerdo'));
