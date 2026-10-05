import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { StockCorteResponse } from './types'

const REFETCH_MS = 15_000 // el mostrador ve el stock bajar de otro dispositivo sin recargar.

/** GET /stock (FR-205, FR-208). */
export function useStock() {
  return useQuery({
    queryKey: ['stock'],
    queryFn: () => apiFetch<StockCorteResponse[]>('/stock'),
    refetchInterval: REFETCH_MS,
  })
}
