import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { PerfilResponse } from './types'

/** GET /perfiles — todas las cuentas, cualquier estado (FR-405). Solo admin (RLS). */
export function usePerfilesTodos(habilitado: boolean) {
  return useQuery({
    queryKey: ['perfiles', 'todas'],
    queryFn: () => apiFetch<PerfilResponse[]>('/perfiles'),
    enabled: habilitado,
  })
}
