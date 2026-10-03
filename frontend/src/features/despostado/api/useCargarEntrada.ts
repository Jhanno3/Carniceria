import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { CargarEntradaRequest, MediaResResponse } from './types'

/** POST /medias-reses — "Cargar entrada" (FR-111). */
export function useCargarEntrada() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: CargarEntradaRequest) =>
      apiFetch<MediaResResponse>('/medias-reses', {
        method: 'POST',
        body: JSON.stringify(request),
      }),
    onSuccess: () => {
      // Una entrada nueva cambia el % histórico que usa la estimación automática.
      queryClient.invalidateQueries({ queryKey: ['estimacion'] })
    },
  })
}
