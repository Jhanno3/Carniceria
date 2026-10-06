import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { Periodo, ReportePeriodoItem } from './types'

/** GET /reportes/por-periodo?desde=&hasta=&periodo= (FR-402). Fechas en formato ISO (yyyy-MM-dd). */
export function useReportePorPeriodo(desde: string, hasta: string, periodo: Periodo) {
  return useQuery({
    queryKey: ['reporte-por-periodo', desde, hasta, periodo],
    queryFn: () =>
      apiFetch<ReportePeriodoItem[]>(`/reportes/por-periodo?desde=${desde}&hasta=${hasta}&periodo=${periodo}`),
  })
}
