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
