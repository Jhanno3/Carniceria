-- Reactiva los 4 cortes que V8__eliminar_cortes_redundantes.sql había sacado por
-- redundantes (decisión del dueño, revertida ahora). Nombres originales (Aguja, Marucha,
-- Pecho, Cogote), pero NO los PLU originales (16/18/19/20): esos ya los usan Espinazo/
-- Roast beef/Chingolo/Tortuga desde V21__cortes_nuevos.sql. Se usan 25-28, los primeros
-- libres después de V21.
--
-- A diferencia de la Fase 1 original (donde Marucha compartía zona con Paleta), acá cada
-- uno tiene su propia zona de mapa — mismo criterio que los 7 cortes de V21, siguiendo
-- resources/mapa-cortes-completo.html, que los dibuja como regiones propias.
insert into cortes (nombre, plu, cuarto, zona_mapa, dueno_id)
select v.nombre, v.plu, v.cuarto, v.zona_mapa, d.dueno_id
from (select distinct dueno_id from cortes) d
cross join (
  values
    ('Aguja', 25, 'Delantero', 'aguja'),
    ('Marucha', 26, 'Delantero', 'marucha'),
    ('Pecho', 27, 'Delantero', 'pecho'),
    ('Cogote', 28, 'Delantero', 'cogote')
) as v(nombre, plu, cuarto, zona_mapa)
on conflict (dueno_id, plu) do nothing;
