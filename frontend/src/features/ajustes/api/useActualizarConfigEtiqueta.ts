import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { ConfigEtiquetaDto } from './types'

/** PUT /config-etiqueta — solo dueño. */
export function useActualizarConfigEtiqueta() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: ConfigEtiquetaDto) =>
      apiFetch<ConfigEtiquetaDto>('/config-etiqueta', {
        method: 'PUT',
        body: JSON.stringify(request),
      }),
    onSuccess: (data) => {
      queryClient.setQueryData(['config-etiqueta'], data)
    },
  })
}
