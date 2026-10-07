-- Fase 7: siembra los 16 cortes de "Achuras y Embutidos" y "Cerdo" para cada negocio que
-- ya tiene catálogo (CatalogoInicialService corrió una sola vez, al aprobarse, y no vuelve
-- a correr para los negocios existentes) — mismo patrón que V21/V22. Un negocio nuevo de
-- acá en más ya los recibe directo del catálogo de ejemplo en Java.
-- "on conflict do nothing" por si algún negocio ya tiene esos PLU ocupados con un corte
-- propio (el dueño ya puede crear/editar cortes, FR-109).
insert into cortes (nombre, plu, cuarto, tipo_producto, zona_mapa, dueno_id)
select v.nombre, v.plu, null, v.tipo_producto, null, d.dueno_id
from (select distinct dueno_id from cortes) d
cross join (
  values
    ('Chinchulín', 29, 'AchurasEmbutidos'),
    ('Molleja', 30, 'AchurasEmbutidos'),
    ('Rabo', 31, 'AchurasEmbutidos'),
    ('Mondongo', 32, 'AchurasEmbutidos'),
    ('Lengua', 33, 'AchurasEmbutidos'),
    ('Riñón', 34, 'AchurasEmbutidos'),
    ('Corazón', 35, 'AchurasEmbutidos'),
    ('Hígado', 36, 'AchurasEmbutidos'),
    ('Fresca', 37, 'AchurasEmbutidos'),
    ('Morcilla Vasca', 38, 'AchurasEmbutidos'),
    ('Morcilla', 39, 'AchurasEmbutidos'),
    ('Chorizo', 40, 'AchurasEmbutidos'),
    ('Bondiola', 41, 'Cerdo'),
    ('Pechito', 42, 'Cerdo'),
    ('Carré', 43, 'Cerdo'),
    ('Cordero', 44, 'Cerdo')
) as v(nombre, plu, tipo_producto)
on conflict (dueno_id, plu) do nothing;
