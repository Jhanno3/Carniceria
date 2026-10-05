-- Fase 3 (FR-307/FR-308): anular una venta nunca es un DELETE, siempre un UPDATE de
-- anulada = true. El dueño ya puede hacerlo por "ventas_dueno_todo" (V13, for all, sin
-- límite de tiempo) — esta política es la que le faltaba al empleado: solo la suya,
-- y solo dentro de los 5 minutos de fecha_hora (hora del servidor, nunca la del
-- dispositivo que hace el pedido — spec.md, caso borde 4.3).
create policy "ventas_empleado_anular" on ventas
  for update
  using (usuario_id = auth.uid() and fecha_hora > now() - interval '5 minutes')
  with check (usuario_id = auth.uid());
