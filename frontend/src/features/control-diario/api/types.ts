// Tipos que reflejan los DTO de contracts/control-diario-api.md. Kilos como `string`
// (nunca `number`), mismo criterio que despostado/api/types.ts.

export interface EscanearRequest {
  codigo: string
  idClienteLocal: string
}

export interface VentaResponse {
  id: string
  fechaHora: string
  corteId: string
  corteNombre: string | null
  kg: string
  codigoLeido: string
  anulada: boolean
}

export interface StockCorteResponse {
  corteId: string
  corteNombre: string | null
  entradoKg: string
  vendidoKg: string
  stockKg: string
  quedaPoco: boolean
}

export interface ResumenDiaResponse {
  kgVendidosHoy: string
  etiquetasEscaneadasHoy: number
  stockVendibleTotal: string
  entradasHoy: number
}
