import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { ReporteCategoriaItem } from './types'

/** GET /reportes/por-categoria?desde=&hasta= (FR-403). Fechas en formato ISO (yyyy-MM-dd). */
export function useReportePorCategoria(desde: string, hasta: string) {
  return useQuery({
    queryKey: ['reporte-por-categoria', desde, hasta],
    queryFn: () => apiFetch<ReporteCategoriaItem[]>(`/reportes/por-categoria?desde=${desde}&hasta=${hasta}`),
  })
}
