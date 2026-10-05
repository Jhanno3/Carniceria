import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { EscanearRequest, VentaResponse } from './types'

/**
 * POST /ventas. El `idClienteLocal` lo genera quien captura el escaneo (CampoEscaneo,
 * Bloque 8), no este hook: así la misma lectura de un lector que dispara el evento dos
 * veces ("doble Enter") puede mandar el mismo id y no duplicar la venta (FR-204,
 * spec.md sección 3.3) — si lo generáramos acá, cada llamada tendría un id distinto.
 */
export function useEscanear() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: EscanearRequest) =>
      apiFetch<VentaResponse>('/ventas', {
        method: 'POST',
        body: JSON.stringify(request),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stock'] })
      queryClient.invalidateQueries({ queryKey: ['resumen-dia'] })
      queryClient.invalidateQueries({ queryKey: ['ventas'] })
    },
  })
}
