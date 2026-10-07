import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { CargarEntradaRequest, MediaResResponse, TipoEntrada } from '../../despostado/api/types'

export interface AnadirStockInput {
  corteId: string
  kg: string
  precioKg: string | null
  tipoEntrada: Extract<TipoEntrada, 'AchurasEmbutidos' | 'Cerdo' | 'Carne'>
}

/** FR-703: alta rápida de stock para cortes que no se despostan de una media res
 * (Achuras/Embutidos, Cerdo) — reutiliza POST /medias-reses tal cual existe, con una
 * "media res" degenerada: pesoKg = el mismo kg cargado, un único corte, pérdidas en cero.
 * Con esos valores el cálculo da rendimientoPorc = 100% y sinAsignarKg = 0 — correcto de
 * verdad (no hay pérdida porque no hay despostado), no es un hack numérico. */
export function useAnadirStock() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ corteId, kg, precioKg, tipoEntrada }: AnadirStockInput) => {
      const request: CargarEntradaRequest = {
        pesoKg: kg,
        precioKg,
        tipoEntrada,
        cortes: [{ corteId, kg }],
        perdidas: { hueso: '0', grasa: '0', merma: '0' },
      }
      return apiFetch<MediaResResponse>('/medias-reses', {
        method: 'POST',
        body: JSON.stringify(request),
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stock'] })
    },
  })
}
