import { formatearPesos } from '../../../shared/formato/formatoEsAr'
import type { Corte } from '../api/types'

interface TablaDeCortesProps {
  cortes: Corte[]
  onEditar: (corte: Corte) => void
  onCambiarActivo: (corte: Corte) => void
  guardando?: boolean
}

/** FR-109/FR-501: lista todos los cortes (activos e inactivos) con sus acciones. */
export function TablaDeCortes({ cortes, onEditar, onCambiarActivo, guardando }: TablaDeCortesProps) {
  if (cortes.length === 0) {
    return <p className="text-cuerpo text-texto-secundario">No hay cortes cargados todavía.</p>
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-borde">
      <table className="w-full text-cuerpo">
        <thead>
          <tr className="border-b border-borde text-etiqueta uppercase text-texto-secundario">
            <th className="p-3 text-left">Nombre</th>
            <th className="p-3 text-left">PLU</th>
            <th className="p-3 text-left">Cuarto</th>
            <th className="p-3 text-left">Zona de mapa</th>
            <th className="p-3 text-left">Precio de venta</th>
            <th className="p-3 text-left">Estado</th>
            <th className="p-3 text-left"></th>
          </tr>
        </thead>
        <tbody>
          {cortes.map((corte) => (
            <tr
              key={corte.id}
              data-testid={`fila-corte-${corte.id}`}
              className={`border-b border-borde last:border-0 ${corte.activo ? '' : 'opacity-50'}`}
            >
              <td className="p-2 font-medium text-texto">{corte.nombre}</td>
              <td className="numero p-2 text-texto-secundario">{corte.plu}</td>
              <td className="p-2 text-texto-secundario">{corte.cuarto ?? '—'}</td>
              <td className="p-2 text-texto-secundario">{corte.zonaMapa ?? '—'}</td>
              <td className="numero p-2 text-texto-secundario">
                {corte.precioVenta ? formatearPesos(Number(corte.precioVenta)) : '—'}
              </td>
              <td className="p-2 text-texto-secundario">{corte.activo ? 'Activo' : 'Inactivo'}</td>
              <td className="p-2 text-right">
                <div className="flex justify-end gap-2">
                  <button
                    type="button"
                    disabled={guardando}
                    onClick={() => onEditar(corte)}
                    className="h-9 rounded-lg border border-borde-campo px-3 text-etiqueta font-medium text-texto disabled:opacity-60"
                  >
                    Editar
                  </button>
                  <button
                    type="button"
                    disabled={guardando}
                    onClick={() => onCambiarActivo(corte)}
                    className="h-9 rounded-lg border border-borde-campo px-3 text-etiqueta font-medium text-texto disabled:opacity-60"
                  >
                    {corte.activo ? 'Desactivar' : 'Activar'}
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
