import { useQuery } from '@tanstack/react-query'
import { ApiError, apiFetch } from '../../../shared/api/apiFetch'
import type { EstimacionResponse } from './types'

/**
 * FR-113: la carga automática solo se ofrece si ya hay al menos una entrada cargada.
 * `disponible: false` cubre tanto "sin historial" (409 SIN_HISTORIAL) como "todavía no
 * se cargó un peso para pedir la estimación".
 */
export function useEstimacion(pesoKg: number | null) {
  const query = useQuery({
    queryKey: ['estimacion', pesoKg],
    queryFn: async () => {
      try {
        return await apiFetch<EstimacionResponse>(`/medias-reses/estimacion?pesoKg=${pesoKg}`)
      } catch (error) {
        if (error instanceof ApiError && error.codigo === 'SIN_HISTORIAL') {
          return null
        }
        throw error
      }
    },
    enabled: pesoKg != null && pesoKg > 0,
  })

  return { ...query, disponible: query.data != null }
}
