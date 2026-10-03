import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { CorteResponse } from './types'

export function useCortes(incluirInactivos = false) {
  return useQuery({
    queryKey: ['cortes', incluirInactivos],
    queryFn: () => apiFetch<CorteResponse[]>(`/cortes?incluirInactivos=${incluirInactivos}`),
  })
}
