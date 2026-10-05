import { formatearKg } from '../../../shared/formato/formatoEsAr'
import type { StockCorteResponse } from '../api/types'

interface TablaStockProps {
  stock: StockCorteResponse[]
}

/** FR-205/FR-208. "Queda poco" nunca depende solo del color (Principio VI): también
 * lleva una etiqueta de texto, mismo criterio que la fila seleccionada de TablaCortes (Fase 1). */
export function TablaStock({ stock }: TablaStockProps) {
  return (
    <div className="overflow-x-auto rounded-xl border border-borde">
      <table className="w-full text-cuerpo">
        <thead>
          <tr className="border-b border-borde text-etiqueta uppercase text-texto-secundario">
            <th className="p-3 text-left">Corte</th>
            <th className="p-3 text-left">Entró</th>
            <th className="p-3 text-left">Vendido</th>
            <th className="p-3 text-left">Queda</th>
          </tr>
        </thead>
        <tbody>
          {stock.map((fila) => {
            const entrado = Number(fila.entradoKg)
            const stockKg = Number(fila.stockKg)
            const porcentajeDisponible = entrado > 0 ? (stockKg / entrado) * 100 : 0
            return (
              <tr
                key={fila.corteId}
                className={`border-b border-borde last:border-0 ${
                  fila.quedaPoco ? 'border-l-4 border-l-aviso-texto bg-aviso-fondo' : ''
                }`}
              >
                <td className="p-2 font-medium text-texto">{fila.corteNombre ?? '—'}</td>
                <td className="numero p-2 text-texto-secundario">{formatearKg(entrado)}</td>
                <td className="numero p-2 text-texto-secundario">{formatearKg(Number(fila.vendidoKg))}</td>
                <td className="p-2">
                  <div className="flex items-center gap-2">
                    <span className="numero text-texto">{formatearKg(stockKg)}</span>
                    {fila.quedaPoco && (
                      <span className="rounded-full bg-aviso-texto px-2 py-0.5 text-etiqueta font-medium text-white">
                        Queda poco
                      </span>
                    )}
                  </div>
                  <div className="mt-1 h-2 w-24 rounded-full bg-fondo-suave">
                    <div
                      className={`h-2 rounded-full ${fila.quedaPoco ? 'bg-aviso-texto' : 'bg-vendible'}`}
                      style={{ width: `${Math.min(100, Math.max(0, porcentajeDisponible))}%` }}
                    />
                  </div>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
