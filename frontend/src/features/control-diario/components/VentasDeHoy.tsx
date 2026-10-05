import { useState } from 'react'
import { formatearKg } from '../../../shared/formato/formatoEsAr'
import type { VentaResponse } from '../api/types'

const CANTIDAD_INICIAL = 5

interface VentasDeHoyProps {
  /** Ya vienen del backend más recientes primero (GET /ventas) — este componente no reordena. */
  ventas: VentaResponse[]
}

function hora(fechaHoraIso: string): string {
  return new Date(fechaHoraIso).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' })
}

/** FR-207: hora, corte y kilos, más reciente arriba, con enlace a ver el listado completo. */
export function VentasDeHoy({ ventas }: VentasDeHoyProps) {
  const [mostrarTodas, setMostrarTodas] = useState(false)
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
              <span className="flex-1 text-texto">{venta.corteNombre ?? '—'}</span>
              <span className="numero text-texto">{formatearKg(Number(venta.kg))}</span>
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
