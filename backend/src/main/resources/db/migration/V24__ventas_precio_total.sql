-- Fase 5, FR-502/FR-503: importe de cada venta, calculado al escanear a partir de
-- cortes.precio_venta. Sin `check`, a diferencia de cortes.precio_venta (V23): puede ser
-- null a propósito (venta registrada sin precio cargado, Principio IV) y una venta vieja
-- nunca lo recalcula retroactivamente si el precio del corte cambia después.
alter table ventas add column precio_total numeric(12, 2);
