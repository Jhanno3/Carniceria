import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import { aCorteRequest } from './aCorteRequest'
import type { Corte, CorteFormValues } from './types'

interface ActualizarCorteInput extends CorteFormValues {
  id: string
}

/** PUT /cortes/{id} — edita nombre/PLU/cuarto/zona/precio de venta/activo (solo dueño). */
export function useActualizarCorte() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, ...valores }: ActualizarCorteInput) =>
      apiFetch<Corte>(`/cortes/${id}`, {
        method: 'PUT',
        body: JSON.stringify(aCorteRequest(valores)),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cortes'] })
    },
  })
}
