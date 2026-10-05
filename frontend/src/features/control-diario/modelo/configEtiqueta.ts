// Dominio puro — espejo de backend/.../escaneo/modelo/ConfigEtiqueta.java. No depende de
// api/types.ts (capa modelo/ no conoce la capa api/, mismo criterio que despostado/modelo/):
// quien llame a decodificar() convierte el ConfigEtiquetaDto ya cacheado a esta forma, que
// resulta ser idéntica campo a campo.
export interface ConfigEtiqueta {
  prefijoDesde: number
  prefijoHasta: number
  inicioPlu: number
  largoPlu: number
  inicioValor: number
  largoValor: number
  tipoValor: 'peso' | 'importe'
  decimales: number
}
