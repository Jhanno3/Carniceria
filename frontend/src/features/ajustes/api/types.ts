// Movido a shared/api/types.ts (plan-fase3.md, Bloque 5): lo necesita también
// `control-diario` para el decodificador offline. Re-exportado acá para no tocar cada
// punto de esta feature que ya importaba `ConfigEtiquetaDto` desde './types'.
export type { ConfigEtiquetaDto } from '../../../shared/api/types'
