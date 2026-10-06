import { formatearPesos } from '../../../shared/formato/formatoEsAr'

export interface FilaReporte {
  grupo: string | null
  cantidadEntradas: number
  rendimientoPromedioPorc: string
  costoKgVendiblePromedio: string | null
  beneficioPorKgVendible: string | null
}

interface TablaReporteProps {
  /** "Proveedor" / "Categoría" / "Período" — título de la primera columna. */
  etiquetaGrupo: string
  /** "Sin proveedor" / "Sin categoría" — cómo se muestra un `grupo` en null (nunca se excluye, spec.md FR-401/FR-403). */
  etiquetaSinValor: string
  filas: FilaReporte[]
}

function formatearPorcentaje(valor: string): string {
  return `${Number(valor).toLocaleString('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} %`
}

/** Fase 4 (spec.md 5.3): sin filas, mensaje neutro, no una tabla vacía sin explicación. */
export function TablaReporte({ etiquetaGrupo, etiquetaSinValor, filas }: TablaReporteProps) {
  if (filas.length === 0) {
    return <p className="text-cuerpo text-texto-secundario">No hay datos para el rango elegido.</p>
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-borde">
      <table className="w-full text-cuerpo">
        <thead>
          <tr className="border-b border-borde text-etiqueta uppercase text-texto-secundario">
            <th className="p-3 text-left">{etiquetaGrupo}</th>
            <th className="p-3 text-left">Entradas</th>
            <th className="p-3 text-left">Rendimiento</th>
            <th className="p-3 text-left">Costo por kg vendible</th>
            <th className="p-3 text-left">Beneficio por kg vendible</th>
          </tr>
        </thead>
        <tbody>
          {filas.map((fila, indice) => (
            <tr key={indice} className="border-b border-borde last:border-0">
              <td className="p-2 font-medium text-texto">{fila.grupo ?? etiquetaSinValor}</td>
              <td className="numero p-2 text-texto-secundario">{fila.cantidadEntradas}</td>
              <td className="numero p-2 text-texto">{formatearPorcentaje(fila.rendimientoPromedioPorc)}</td>
              <td className="numero p-2 text-texto">
                {fila.costoKgVendiblePromedio != null ? formatearPesos(Number(fila.costoKgVendiblePromedio)) : '—'}
              </td>
              <td className="numero p-2 text-texto">
                {fila.beneficioPorKgVendible != null ? formatearPesos(Number(fila.beneficioPorKgVendible)) : '—'}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
