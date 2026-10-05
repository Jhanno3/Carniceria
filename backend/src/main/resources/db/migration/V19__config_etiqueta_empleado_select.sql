-- Fase 3 (plan-fase3.md, 3.1b): el decodificador EAN-13 se duplica en el frontend para
-- el modo offline, y corre en la sesión de quien escanea, dueño o empleado. Para que un
-- empleado pueda cachear la config_etiqueta de su negocio mientras hay conexión (y
-- decodificar con eso cuando se corta), necesita poder leerla. Revierte la restricción de
-- plan-fase2.md ("el empleado nunca necesita leerla directo") sin comprometer el
-- Principio II: ninguna columna de config_etiqueta es un precio, costo, ni dato sensible.
create policy "config_etiqueta_empleado_select" on config_etiqueta
  for select
  using (dueno_id = mi_negocio_id());
