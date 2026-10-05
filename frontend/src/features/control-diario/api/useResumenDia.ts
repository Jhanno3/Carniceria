import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { ResumenDiaResponse } from './types'

const REFETCH_MS = 15_000

/** GET /control-diario/resumen?fecha= (FR-206). Sin `fecha`, el backend usa hoy (Argentina). */
export function useResumenDia(fecha?: string) {
  return useQuery({
    queryKey: ['resumen-dia', fecha ?? 'hoy'],
    queryFn: () => apiFetch<ResumenDiaResponse>(`/control-diario/resumen${fecha ? `?fecha=${fecha}` : ''}`),
    refetchInterval: REFETCH_MS,
  })
}
