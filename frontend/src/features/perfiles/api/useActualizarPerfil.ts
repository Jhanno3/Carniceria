import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { PerfilResponse } from './types'

interface ActualizarPerfilInput {
  id: string
  rol: 'admin' | 'dueno' | 'empleado'
  estado: 'pendiente' | 'aprobado' | 'rechazado' | 'pausado'
}

/** PUT /perfiles/{id} — aprobar, rechazar, pausar/reactivar, o cambiar el rol de una cuenta (solo admin). */
export function useActualizarPerfil() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, rol, estado }: ActualizarPerfilInput) =>
      apiFetch<PerfilResponse>(`/perfiles/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ rol, estado }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['perfiles'] })
    },
  })
}
