import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { PerfilResponse } from './types'

/** GET /perfiles?estado=pendiente — solo dueño (RLS). */
export function usePerfilesPendientes(habilitado: boolean) {
  return useQuery({
    queryKey: ['perfiles', 'pendiente'],
    queryFn: () => apiFetch<PerfilResponse[]>('/perfiles?estado=pendiente'),
    enabled: habilitado,
  })
}
