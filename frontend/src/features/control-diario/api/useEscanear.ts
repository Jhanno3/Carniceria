import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiFetch, ApiError } from '../../../shared/api/apiFetch'
import { useCortes } from '../../../shared/api/useCortes'
import { useConfigEtiqueta } from '../../../shared/api/useConfigEtiqueta'
import type { CorteResponse, ConfigEtiquetaDto } from '../../../shared/api/types'
import { decodificar } from '../modelo/decodificadorEtiqueta'
import type { ResultadoDecodificacion } from '../modelo/decodificadorEtiqueta'
import { agregar as agregarALaCola } from '../cola/colaVentas'
import type { EscanearRequest, VentaResponse, VentaConfirmadaOPendiente } from './types'

// Mismos códigos/mensajes que usaría el backend (contracts/control-diario-api.md), para
// que UltimoEscaneo no necesite un mapa de mensajes aparte para el camino offline.
const CODIGO_POR_VARIANTE: Record<Exclude<ResultadoDecodificacion['tipo'], 'Exito'>, string> = {
  DigitoVerificadorInvalido: 'DIGITO_VERIFICADOR_INVALIDO',
  PrefijoInvalido: 'PREFIJO_INVALIDO',
  PesoCero: 'PESO_CERO',
}

async function decodificarYEncolar(
  request: EscanearRequest,
  cortes: CorteResponse[] | undefined,
  config: ConfigEtiquetaDto | undefined,
): Promise<VentaConfirmadaOPendiente> {
  if (!cortes || !config) {
    // Caso borde de plan-fase3.md 3.1b: la pantalla se abrió sin haber tenido conexión
    // nunca (no se cacheó ni el catálogo ni la config) — no hay con qué decodificar.
    throw new ApiError(
      'SIN_CONEXION_SIN_CACHE',
      'Sin conexión y sin datos para trabajar offline — conectate al menos una vez.',
      0,
    )
  }

  const resultado = decodificar(request.codigo, config)
  if (resultado.tipo !== 'Exito') {
    throw new ApiError(CODIGO_POR_VARIANTE[resultado.tipo], 'El código escaneado no es válido.', 400)
  }

  const corte = cortes.find((c) => c.plu === resultado.plu && c.activo)
  if (!corte) {
    throw new ApiError('PLU_INEXISTENTE', 'El código no corresponde a ningún corte activo.', 400)
  }

  await agregarALaCola({
    idClienteLocal: request.idClienteLocal,
    codigo: request.codigo,
    decodificado: { corteId: corte.id, corteNombre: corte.nombre, kg: resultado.kg },
    creadoEn: new Date().toISOString(),
  })

  return { corteNombre: corte.nombre, kg: resultado.kg, pendiente: true }
}

/**
 * POST /ventas, con fallback offline (plan-fase3.md, 3.3). El `idClienteLocal` lo genera
 * quien captura el escaneo (CampoEscaneo), no este hook: así la misma lectura de un
 * lector que dispara el evento dos veces ("doble Enter") puede mandar el mismo id y no
 * duplicar la venta, tanto online (FR-204) como una vez que sale de la cola offline.
 *
 * Intenta la red primero, igual que en Fase 2 — el decodificador local solo entra en
 * juego si `fetch` falla por conexión (nunca ante un error de negocio real del backend,
 * que ya decodificó con la config vigente y sabe mejor que el dispositivo offline).
 */
export function useEscanear() {
  const queryClient = useQueryClient()
  const cortes = useCortes()
  const config = useConfigEtiqueta()

  return useMutation({
    mutationFn: async (request: EscanearRequest): Promise<VentaConfirmadaOPendiente> => {
      try {
        const venta = await apiFetch<VentaResponse>('/ventas', {
          method: 'POST',
          body: JSON.stringify(request),
        })
        return { corteNombre: venta.corteNombre, kg: Number(venta.kg), pendiente: false }
      } catch (error) {
        if (error instanceof ApiError) {
          throw error
        }
        return await decodificarYEncolar(request, cortes.data, config.data)
      }
    },
    onSuccess: (resultado) => {
      if (resultado.pendiente) {
        queryClient.invalidateQueries({ queryKey: ['cola-pendiente'] })
        return
      }
      queryClient.invalidateQueries({ queryKey: ['stock'] })
      queryClient.invalidateQueries({ queryKey: ['resumen-dia'] })
      queryClient.invalidateQueries({ queryKey: ['ventas'] })
    },
  })
}
