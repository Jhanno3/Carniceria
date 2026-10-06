-- Fase 6, FR-601: qué parte de la media res llegó como entrada (a veces es la media res
-- entera, a veces ya es un corte comercial más chico, como un "pecho" o un "mocho"). A
-- diferencia de categoria (nullable, "no especificado" es válido), acá toda entrada es
-- "algo" — el default resuelve las filas viejas, que nunca restringían nada.
alter table medias_reses add column tipo_entrada text not null default 'MediaRes'
  check (tipo_entrada in ('MediaRes', 'Delantero', 'Pecho', 'Parrillero', 'AsadoCompleto', 'Mocho', 'Rueda'));
