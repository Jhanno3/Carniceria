import { formatearKg } from '../../../shared/formato/formatoEsAr'
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

/** FR-206: kilos vendidos hoy, etiquetas escaneadas, stock vendible total, entradas de hoy. */
export function ResumenDia({ resumen }: ResumenDiaProps) {
  return (
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
    </div>
  )
}
