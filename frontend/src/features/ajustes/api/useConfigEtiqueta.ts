import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { ConfigEtiquetaDto } from './types'

/** GET /config-etiqueta — solo dueño. */
export function useConfigEtiqueta() {
  return useQuery({
    queryKey: ['config-etiqueta'],
    queryFn: () => apiFetch<ConfigEtiquetaDto>('/config-etiqueta'),
  })
}
