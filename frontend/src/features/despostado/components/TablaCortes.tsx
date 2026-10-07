import type { CorteResponse } from '../api/types'

interface TablaCortesProps {
  cortes: CorteResponse[]
  kgPorCorte: Record<string, string>
  onCambiarKg: (corteId: string, texto: string) => void
  corteSeleccionadoId: string | null
  onSeleccionarCorte: (corteId: string) => void
  /** Fase 6, FR-602: `null` (o ausente) = sin restricción ("Media res"), nada se atenúa. */
  corteNombresHabilitados?: Set<string> | null
}

export function TablaCortes({
  cortes,
  kgPorCorte,
  onCambiarKg,
  corteSeleccionadoId,
  onSeleccionarCorte,
  corteNombresHabilitados = null,
}: TablaCortesProps) {
  const kgMaximo = Math.max(0, ...cortes.map((c) => Number(kgPorCorte[c.id] || 0)));

  return (
    <table className="w-full text-cuerpo">
      <thead className="sticky top-0 z-10 bg-fondo">
        <tr className="border-b border-borde text-etiqueta uppercase text-texto-secundario">
          <th className="p-3 text-left">Corte</th>
          <th className="p-3 text-left">Cuarto</th>
          <th className="p-3 text-left">Kilos</th>
          <th className="p-3 text-left">%</th>
          <th className="p-3 text-left">Peso relativo</th>
        </tr>
      </thead>
      <tbody>
        {cortes.map((corte) => {
          const kgTexto = kgPorCorte[corte.id] ?? ''
          const kg = Number(kgTexto) || 0
          const seleccionada = corte.id === corteSeleccionadoId
          const habilitado = corteNombresHabilitados === null || corteNombresHabilitados.has(corte.nombre)
          return (
            <tr
              key={corte.id}
              data-testid={`fila-corte-${corte.id}`}
              aria-selected={seleccionada}
              className={`border-b border-borde/60 last:border-0 hover:bg-fondo-suave-2 ${seleccionada ? 'bg-rosado border-l-4 border-l-vendible' : ''} ${habilitado ? '' : 'opacity-50'}`}
            >
              <td className="p-2">
                <button
                  type="button"
                  onClick={() => onSeleccionarCorte(corte.id)}
                  aria-pressed={seleccionada}
                  className={`h-11 px-2 text-left underline-offset-2 hover:underline ${seleccionada ? 'font-bold text-vendible' : 'font-medium text-vendible'}`}
                >
                  {corte.nombre}
                </button>
              </td>
              <td className="p-2 text-texto-secundario">{corte.cuarto ?? '—'}</td>
              <td className="p-2">
                <label htmlFor={`kg-${corte.id}`} className="sr-only">
                  Kilos de {corte.nombre}
                </label>
                <input
                  id={`kg-${corte.id}`}
                  type="text"
                  inputMode="decimal"
                  value={kgTexto}
                  disabled={!habilitado}
                  onChange={(e) => onCambiarKg(corte.id, e.target.value)}
                  className="numero h-11 w-24 rounded-xl border border-borde-campo px-2 disabled:bg-fondo-suave disabled:text-texto-secundario"
                />
              </td>
              <td className="numero p-2 text-texto-secundario">
                {kgMaximo > 0 ? ((kg / kgMaximo) * 100).toFixed(0) : '0'} %
              </td>
              <td className="p-2">
                <div className="h-2 w-24 rounded-full bg-fondo-suave">
                  <div
                    className="h-2 rounded-full bg-vendible"
                    style={{ width: `${kgMaximo > 0 ? (kg / kgMaximo) * 100 : 0}%` }}
                  />
                </div>
              </td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}
