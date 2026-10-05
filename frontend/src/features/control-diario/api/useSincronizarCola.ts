import { useEffect, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../../../shared/api/apiFetch'
import { listar, eliminar } from '../cola/colaVentas'
import type { EscanearRequest, VentaResponse } from './types'

const INTERVALO_MS = 30_000

export interface VentaNoSincronizada {
  codigo: string
  mensaje: string
}

/**
 * Sincroniza la cola offline al volver la conexión (plan-fase3.md, 3.4): recorre los
 * escaneos pendientes en orden de creación, uno a la vez. No es un hook de datos (no
 * devuelve una query) — se monta una sola vez desde ControlDiarioPage y corre en segundo
 * plano; devuelve la lista de ventas que no se pudieron sincronizar para avisarle al
 * usuario.
 */
export function useSincronizarCola() {
  const queryClient = useQueryClient()
  const [noSincronizadas, setNoSincronizadas] = useState<VentaNoSincronizada[]>([])

  useEffect(() => {
    let cancelado = false

    async function sincronizar() {
      const pendientes = await listar()
      for (const pendiente of pendientes) {
        if (cancelado) return

        const request: EscanearRequest = { codigo: pendiente.codigo, idClienteLocal: pendiente.idClienteLocal }
        try {
          await apiFetch<VentaResponse>('/ventas', { method: 'POST', body: JSON.stringify(request) })
        } catch (error) {
          if (!(error instanceof ApiError)) {
            // Seguimos sin conexión real: se corta esta pasada, queda en la cola para la
            // próxima (evento `online` o el intervalo de abajo).
            return
          }
          // Error real de negocio (p. ej. la config cambió mientras estaba offline): no
          // tiene sentido reintentar algo que va a fallar siempre igual.
          setNoSincronizadas((previas) => [...previas, { codigo: pendiente.codigo, mensaje: error.message }])
        }

        await eliminar(pendiente.idClienteLocal)
        if (!cancelado) {
          queryClient.invalidateQueries({ queryKey: ['cola-pendiente'] })
        }
      }

      if (!cancelado) {
        queryClient.invalidateQueries({ queryKey: ['stock'] })
        queryClient.invalidateQueries({ queryKey: ['resumen-dia'] })
        queryClient.invalidateQueries({ queryKey: ['ventas'] })
      }
    }

    sincronizar()
    window.addEventListener('online', sincronizar)
    const intervalo = setInterval(() => {
      if (navigator.onLine) sincronizar()
    }, INTERVALO_MS)

    return () => {
      cancelado = true
      window.removeEventListener('online', sincronizar)
      clearInterval(intervalo)
    }
  }, [queryClient])

  return { noSincronizadas }
}
