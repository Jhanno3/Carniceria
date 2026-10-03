-- Aguja, Marucha, Pecho y Cogote se sacan del catálogo: en la práctica de esta
-- carnicería no se despostan como cortes aparte, van incluidos dentro de otros
-- cortes ya existentes (Paleta/Asado). Sin uso en `despostado` a la fecha de esta
-- migración, así que el borrado es seguro (no rompe ninguna fila histórica).
delete from cortes where plu in (16, 18, 19, 20);
