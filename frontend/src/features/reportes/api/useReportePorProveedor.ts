import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { ReporteProveedorItem } from './types'

/** GET /reportes/por-proveedor?desde=&hasta= (FR-401). Fechas en formato ISO (yyyy-MM-dd). */
export function useReportePorProveedor(desde: string, hasta: string) {
  return useQuery({
    queryKey: ['reporte-por-proveedor', desde, hasta],
    queryFn: () => apiFetch<ReporteProveedorItem[]>(`/reportes/por-proveedor?desde=${desde}&hasta=${hasta}`),
  })
}
