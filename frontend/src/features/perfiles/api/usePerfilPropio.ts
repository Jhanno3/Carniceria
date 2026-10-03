import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { PerfilResponse } from './types'

/** GET /perfiles/yo — crea el perfil (pendiente) la primera vez que se llama. */
export function usePerfilPropio(habilitado: boolean) {
  return useQuery({
    queryKey: ['perfil-propio'],
    queryFn: () => apiFetch<PerfilResponse>('/perfiles/yo'),
    enabled: habilitado,
  })
}
