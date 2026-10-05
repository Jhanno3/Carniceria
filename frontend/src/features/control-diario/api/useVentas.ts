import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { VentaResponse } from './types'

const REFETCH_MS = 15_000 // el mostrador ve ventas de otro dispositivo sin recargar.

/** GET /ventas?desde=&hasta=&limite= — fechas en formato ISO (yyyy-MM-dd). */
export function useVentas(desde: string, hasta: string, limite?: number) {
  return useQuery({
    queryKey: ['ventas', desde, hasta, limite ?? null],
    queryFn: () => {
      const limiteParte = limite != null ? `&limite=${limite}` : ''
      return apiFetch<VentaResponse[]>(`/ventas?desde=${desde}&hasta=${hasta}${limiteParte}`)
    },
    refetchInterval: REFETCH_MS,
  })
}
