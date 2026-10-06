import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import { aCorteRequest } from './aCorteRequest'
import type { Corte, CorteFormValues } from './types'

/** POST /cortes — alta de un corte nuevo (cierra el gap de FR-109, incluye FR-501). */
export function useCrearCorte() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (valores: CorteFormValues) =>
      apiFetch<Corte>('/cortes', {
        method: 'POST',
        body: JSON.stringify(aCorteRequest(valores)),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cortes'] })
    },
  })
}
