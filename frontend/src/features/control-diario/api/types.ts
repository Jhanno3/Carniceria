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
  usuarioId: string
}

// Lo que devuelve `useEscanear` (Fase 3): un escaneo puede quedar confirmado por el
// backend o, sin conexión, "pendiente de sincronizar" con el resultado del decodificador
// local — misma forma para que `UltimoEscaneo` no necesite saber de cuál se trata
// (plan-fase3.md, 3.3).
export interface VentaConfirmadaOPendiente {
  corteNombre: string | null
  kg: number
  pendiente: boolean
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
