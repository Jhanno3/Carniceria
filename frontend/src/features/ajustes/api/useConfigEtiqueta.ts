// Movido a shared/api/useConfigEtiqueta.ts (plan-fase3.md, Bloque 5): el GET lo necesita
// también `control-diario` para el decodificador offline (ya no es "solo dueño" desde
// V19__config_etiqueta_empleado_select.sql). Re-exportado acá para no tocar cada punto de
// esta feature que ya importaba desde './api/useConfigEtiqueta'. El PUT sigue siendo solo
// de esta feature — ver useActualizarConfigEtiqueta.ts.
export { useConfigEtiqueta } from '../../../shared/api/useConfigEtiqueta'
