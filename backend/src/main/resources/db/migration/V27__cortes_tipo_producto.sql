-- Fase 7: distingue cortes que salen de despostar una media res ("Vacuno", el
-- comportamiento de siempre) de los que se compran ya terminados (Achuras/Embutidos,
-- Cerdo). "cuarto" es una clasificación anatómica de la media res — solo tiene sentido
-- para "Vacuno"; para los otros dos, siempre es null.
alter table cortes add column tipo_producto text not null default 'Vacuno'
  check (tipo_producto in ('Vacuno', 'AchurasEmbutidos', 'Cerdo'));

alter table cortes alter column cuarto drop not null;

-- El nombre de la constraint original (V2__cortes.sql, "cuarto text not null check (...)"
-- inline) no se registró a mano en ningún lado de este repo; se busca en pg_constraint en
-- vez de asumir el nombre que Postgres le puso solo (mismo criterio que V20).
do $$
declare
  nombre_constraint text;
begin
  select conname into nombre_constraint
  from pg_constraint
  where conrelid = 'cortes'::regclass
    and contype = 'c'
    and pg_get_constraintdef(oid) ilike '%cuarto%delantero%trasero%ambos%';
  if nombre_constraint is not null then
    execute format('alter table cortes drop constraint %I', nombre_constraint);
  end if;
end $$;

alter table cortes add constraint cortes_cuarto_check check (
  (tipo_producto = 'Vacuno' and cuarto in ('Delantero', 'Trasero', 'Ambos'))
  or (tipo_producto <> 'Vacuno' and cuarto is null)
);
