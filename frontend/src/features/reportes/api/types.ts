// Tipos que reflejan los DTO de fase4/contracts/reportes-api.md. Kilos/importes como
// `string` (nunca `number`), mismo criterio que el resto de la API.

export interface ReporteProveedorItem {
  proveedor: string | null
  cantidadEntradas: number
  rendimientoPromedioPorc: string
  costoKgVendiblePromedio: string | null
  beneficioPorKgVendible: string | null
}

export interface ReporteCategoriaItem {
  categoria: string | null
  cantidadEntradas: number
  rendimientoPromedioPorc: string
  costoKgVendiblePromedio: string | null
  beneficioPorKgVendible: string | null
}

export type Periodo = 'dia' | 'semana' | 'mes'

export interface ReportePeriodoItem {
  periodoInicio: string
  cantidadEntradas: number
  rendimientoPromedioPorc: string
  costoKgVendiblePromedio: string | null
  beneficioPorKgVendible: string | null
}
