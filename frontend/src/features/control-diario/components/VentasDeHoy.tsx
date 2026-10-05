import { useState } from 'react'
import { formatearKg } from '../../../shared/formato/formatoEsAr'
import { useAnularVenta } from '../api/useAnularVenta'
import type { VentaResponse } from '../api/types'

const CANTIDAD_INICIAL = 5
const VENTANA_ANULACION_MS = 5 * 60 * 1000

interface VentasDeHoyProps {
  /** Ya vienen del backend más recientes primero (GET /ventas) — este componente no reordena. */
  ventas: VentaResponse[]
  usuarioActualId: string
  /** Dueño/admin: puede anular cualquier venta del negocio, sin límite de tiempo (FR-308). */
  puedeAnularCualquiera: boolean
}

function hora(fechaHoraIso: string): string {
  return new Date(fechaHoraIso).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' })
}

// FR-308: un empleado solo puede anular una venta propia de menos de 5 minutos. Se evalúa
// contra el reloj del propio dispositivo, solo para decidir si mostrar el botón — la
// autoridad real es la política RLS del backend, que usa la hora del servidor
// (plan-fase3.md, 3.6). No se renderiza el botón si de antemano sabemos que va a fallar,
// en vez de dejarlo deshabilitado (evita un click que solo termina en error).
function puedeAnular(venta: VentaResponse, usuarioActualId: string, puedeAnularCualquiera: boolean): boolean {
  if (venta.anulada) return false
  if (puedeAnularCualquiera) return true
  if (venta.usuarioId !== usuarioActualId) return false
  return Date.now() - new Date(venta.fechaHora).getTime() < VENTANA_ANULACION_MS
}

/** FR-207: hora, corte y kilos, más reciente arriba, con enlace a ver el listado completo. */
export function VentasDeHoy({ ventas, usuarioActualId, puedeAnularCualquiera }: VentasDeHoyProps) {
  const [mostrarTodas, setMostrarTodas] = useState(false)
  const anular = useAnularVenta()
  const visibles = mostrarTodas ? ventas : ventas.slice(0, CANTIDAD_INICIAL)

  return (
    <div className="flex flex-col gap-2 rounded-xl border border-borde p-4">
      <h3 className="text-titulo-seccion font-titulos font-bold text-texto">Ventas de hoy</h3>

      {ventas.length === 0 ? (
        <p className="text-cuerpo text-texto-secundario">Todavía no hay ventas hoy.</p>
      ) : (
        <ul className="flex flex-col divide-y divide-borde">
          {visibles.map((venta) => (
            <li key={venta.id} className="flex items-center justify-between gap-3 py-2 text-cuerpo">
              <span className="text-texto-secundario">{hora(venta.fechaHora)}</span>
              <span className={`flex-1 ${venta.anulada ? 'text-texto-secundario line-through' : 'text-texto'}`}>
                {venta.corteNombre ?? '—'}
              </span>
              <span className={`numero ${venta.anulada ? 'text-texto-secundario line-through' : 'text-texto'}`}>
                {formatearKg(Number(venta.kg))}
              </span>
              {venta.anulada ? (
                <span className="text-etiqueta font-medium uppercase text-texto-secundario">Anulada</span>
              ) : (
                puedeAnular(venta, usuarioActualId, puedeAnularCualquiera) && (
                  <button
                    type="button"
                    disabled={anular.isPending}
                    onClick={() => anular.mutate(venta.id)}
                    className="h-9 rounded-lg border border-borde-campo px-3 text-etiqueta font-medium text-texto disabled:opacity-60"
                  >
                    Anular
                  </button>
                )
              )}
            </li>
          ))}
        </ul>
      )}

      {!mostrarTodas && ventas.length > CANTIDAD_INICIAL && (
        <button
          type="button"
          onClick={() => setMostrarTodas(true)}
          className="h-11 self-start text-cuerpo text-texto-secundario underline"
        >
          Ver todas
        </button>
      )}
    </div>
  )
}
