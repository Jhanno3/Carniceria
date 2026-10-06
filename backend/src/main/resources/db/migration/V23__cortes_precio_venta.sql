-- Fase 5, FR-501: precio de venta ($/kg) de cada corte, editable por el dueño desde
-- "Editar cortes". Opcional (una venta puede registrarse sin precio cargado, Principio IV)
-- y positivo cuando está cargado — mismo criterio que medias_reses.precio_kg (V4).
alter table cortes add column precio_venta numeric(12, 2) check (precio_venta is null or precio_venta > 0);
