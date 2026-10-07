// Tipos de API usados por más de una feature (plan-fase3.md, Bloque 5): `cortes` lo
// necesitan `despostado` y `control-diario`; `config_etiqueta` (de solo lectura) lo
// necesitan `ajustes` y, desde Fase 3, `control-diario` para decodificar offline.

export interface CorteResponse {
  id: string
  nombre: string
  plu: number
  // null solo cuando tipoProducto no es "Vacuno" (Fase 7) — no aplica fuera de la media res.
  cuarto: 'Delantero' | 'Trasero' | 'Ambos' | null
  tipoProducto: 'Vacuno' | 'AchurasEmbutidos' | 'Cerdo' | 'Carne'
  zonaMapa: string | null
  activo: boolean
  /** $/kg (Fase 5, FR-501) — precio que se le cobra al cliente, nunca un costo. */
  precioVenta: string | null
}

// Ver contracts/control-diario-api.md, "GET/PUT /config-etiqueta" (FR-209).
export interface ConfigEtiquetaDto {
  prefijoDesde: number
  prefijoHasta: number
  inicioPlu: number
  largoPlu: number
  inicioValor: number
  largoValor: number
  tipoValor: 'peso' | 'importe'
  decimales: number
}
