-- Resuelve la pregunta abierta de Fase 4 (spec.md, sección 5.3 / 7): "por categoría de
-- animal" necesitaba un campo que no existía. Clasificación típica de Mercado de Liniers,
-- a elección del dueño al cargar la media res — opcional (muchas entradas viejas no lo
-- van a tener, y no es obligatorio saberlo para despostar).
alter table medias_reses
  add column categoria text check (categoria in ('Novillo', 'Novillito', 'Vaquillona', 'Vaca', 'Toro', 'Ternero'));
