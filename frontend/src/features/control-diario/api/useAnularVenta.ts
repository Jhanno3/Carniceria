import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { VentaResponse } from './types'

/** POST /ventas/{id}/anular (FR-307/FR-308). Anular le devuelve el kg al stock, mismo criterio que una venta nueva lo descuenta. */
export function useAnularVenta() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => apiFetch<VentaResponse>(`/ventas/${id}/anular`, { method: 'POST' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ventas'] })
      queryClient.invalidateQueries({ queryKey: ['stock'] })
      queryClient.invalidateQueries({ queryKey: ['resumen-dia'] })
    },
  })
}
