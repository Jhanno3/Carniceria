import { useQuery } from '@tanstack/react-query'
import { apiFetch } from './apiFetch'
import type { CorteResponse } from './types'

// Compartido entre `despostado` y `control-diario` (plan-fase3.md, Bloque 5: el
// decodificador offline necesita el catálogo cacheado para resolver PLU → corte).
export function useCortes(incluirInactivos = false) {
  return useQuery({
    queryKey: ['cortes', incluirInactivos],
    queryFn: () => apiFetch<CorteResponse[]>(`/cortes?incluirInactivos=${incluirInactivos}`),
  })
}
