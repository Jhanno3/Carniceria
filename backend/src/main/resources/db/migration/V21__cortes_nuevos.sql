-- 7 cortes nuevos (resources/mapa-cortes-completo.html, mapa de cortes más detallado),
-- a pedido del dueño — ver también CatalogoInicialService.CATALOGO_DE_EJEMPLO para que
-- los negocios nuevos los sigan recibiendo de acá en más. Se descartó agregar los otros 5
-- cortes que aparecían en ese archivo (Bife de chorizo: sinónimo de "Bife angosto", ya
-- existente; Marucha/Aguja/Pecho/Cogote: sacados a propósito en
-- V8__eliminar_cortes_redundantes.sql, no se revierte esa decisión).
--
-- PLU: se reusan 16/18/19/20 (quedaron libres cuando V8 sacó Aguja/Marucha/Pecho/Cogote)
-- y se suman 22/23/24 para los que no entran en los libres.
--
-- Para cada negocio que YA tiene su catálogo sembrado (CatalogoInicialService corrió una
-- sola vez, al aprobarse, y no vuelve a correr) hay que insertarlos acá explícitamente —
-- un negocio nuevo de acá en más ya los recibe directo del catálogo de ejemplo en Java.
-- "on conflict do nothing" por si algún negocio ya tiene esos PLU ocupados con un corte
-- propio (el dueño ya puede crear/editar cortes, FR-109).
insert into cortes (nombre, plu, cuarto, zona_mapa, dueno_id)
select v.nombre, v.plu, v.cuarto, v.zona_mapa, d.dueno_id
from (select distinct dueno_id from cortes) d
cross join (
  values
    ('Espinazo', 16, 'Ambos', 'espinazo'),
    ('Roast beef', 18, 'Delantero', 'roastBeef'),
    ('Chingolo', 19, 'Delantero', 'chingolo'),
    ('Tortuga', 20, 'Trasero', 'tortuga'),
    ('Colita de cuadril', 22, 'Trasero', 'colitaCuadril'),
    ('Entraña', 23, 'Delantero', 'entrana'),
    ('Asado americano', 24, 'Delantero', 'asadoAmericano')
) as v(nombre, plu, cuarto, zona_mapa)
on conflict (dueno_id, plu) do nothing;
