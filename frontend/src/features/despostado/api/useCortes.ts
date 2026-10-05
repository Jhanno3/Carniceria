// Movido a shared/api/useCortes.ts (plan-fase3.md, Bloque 5): lo necesita también
// `control-diario` para el decodificador offline. Re-exportado acá para no tocar cada
// punto de esta feature que ya importaba desde './api/useCortes'.
export { useCortes } from '../../../shared/api/useCortes'
