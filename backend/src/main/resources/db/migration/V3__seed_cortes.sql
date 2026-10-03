-- 21 cortes de especificacion-carniceria.md, sección 2.2.
-- PLU provisorio (a reemplazar por el dueño cuando configure la balanza real) —
-- se usa 12 para Vacío porque coincide con el ejemplo de etiqueta de la sección 7.
insert into cortes (nombre, plu, cuarto, zona_mapa) values
  ('Cuadril',               1,  'Trasero',    'cuadril'),
  ('Lomo',                  2,  'Trasero',    'lomo'),
  ('Bife angosto',          3,  'Trasero',    'bifeAngosto'),
  ('Nalga',                 4,  'Trasero',    'nalga'),
  ('Cuadrada',               5,  'Trasero',    'nalga'),
  ('Tapa de nalga',          6,  'Trasero',    'nalga'),
  ('Bola de lomo',           7,  'Trasero',    'bola'),
  ('Peceto',                 8,  'Trasero',    'bola'),
  ('Vacío',                  12, 'Trasero',    'vacio'),
  ('Matambre',               9,  'Trasero',    'matambre'),
  ('Osobuco',                10, 'Ambos',      'osobuco'),
  ('Asado',                  11, 'Delantero',  'asado'),
  ('Tapa de asado',          13, 'Delantero',  'asado'),
  ('Falda',                  14, 'Delantero',  'falda'),
  ('Bife ancho',             15, 'Delantero',  'bifeAncho'),
  ('Aguja',                  16, 'Delantero',  'aguja'),
  ('Paleta',                 17, 'Delantero',  'paleta'),
  ('Marucha',                18, 'Delantero',  'paleta'),
  ('Pecho',                  19, 'Delantero',  'pecho'),
  ('Cogote',                 20, 'Delantero',  'cogote'),
  ('Carne picada / recortes',21, 'Ambos',      null);
