import { formatearKg, formatearPesos } from '../../../shared/formato/formatoEsAr'
import type { ResumenDiaResponse } from '../api/types'

const FORMATO_ENTERO = new Intl.NumberFormat('es-AR')

interface ResumenDiaProps {
  resumen: ResumenDiaResponse
}

function Tarjeta({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-2 rounded-xl border border-borde p-4">
      <h3 className="text-etiqueta font-medium uppercase text-texto-secundario">{titulo}</h3>
      {children}
    </div>
  )
}

/** FR-206/FR-503: kilos vendidos hoy, etiquetas escaneadas, stock vendible, entradas y recaudado hoy. */
export function ResumenDia({ resumen }: ResumenDiaProps) {
  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Tarjeta titulo="Kilos vendidos hoy">
          <p className="numero text-cifra-tarjeta font-titulos font-bold text-vendible">
            {formatearKg(Number(resumen.kgVendidosHoy))}
          </p>
        </Tarjeta>
        <Tarjeta titulo="Etiquetas escaneadas">
          <p className="numero text-cifra-tarjeta font-titulos font-bold text-texto">
            {FORMATO_ENTERO.format(resumen.etiquetasEscaneadasHoy)}
          </p>
        </Tarjeta>
        <Tarjeta titulo="Stock vendible en cámara">
          <p className="numero text-cifra-tarjeta font-titulos font-bold text-texto">
            {formatearKg(Number(resumen.stockVendibleTotal))}
          </p>
        </Tarjeta>
        <Tarjeta titulo="Medias reses cargadas hoy">
          <p className="numero text-cifra-tarjeta font-titulos font-bold text-texto">
            {FORMATO_ENTERO.format(resumen.entradasHoy)}
          </p>
        </Tarjeta>
        <Tarjeta titulo="Recaudado hoy">
          <p className="numero text-cifra-tarjeta font-titulos font-bold text-vendible">
            {formatearPesos(Number(resumen.dineroRecaudadoHoy))}
          </p>
        </Tarjeta>
      </div>

      {resumen.ventasSinPrecioHoy > 0 && (
        <p className="text-cuerpo text-texto-secundario">
          {resumen.ventasSinPrecioHoy} ventas de hoy sin precio registrado.
        </p>
      )}
    </div>
  )
}
