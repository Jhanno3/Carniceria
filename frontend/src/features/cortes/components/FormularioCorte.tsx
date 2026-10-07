import { useState } from 'react'
import type { FormEvent } from 'react'
import type { Corte, CorteFormValues } from '../api/types'

const CUARTOS: CorteFormValues['cuarto'][] = ['Delantero', 'Trasero', 'Ambos']
const TIPOS_DE_PRODUCTO: { valor: CorteFormValues['tipoProducto']; etiqueta: string }[] = [
  { valor: 'Vacuno', etiqueta: 'Vacuno' },
  { valor: 'AchurasEmbutidos', etiqueta: 'Achuras y Embutidos' },
  { valor: 'Cerdo', etiqueta: 'Cerdo' },
  { valor: 'Carne', etiqueta: 'Carne' },
]

function valoresIniciales(corteExistente: Corte | null): CorteFormValues {
  if (!corteExistente) {
    return {
      nombre: '',
      plu: '',
      cuarto: 'Ambos',
      tipoProducto: 'Vacuno',
      zonaMapa: '',
      activo: true,
      precioVenta: '',
    }
  }
  return {
    nombre: corteExistente.nombre,
    plu: String(corteExistente.plu),
    // Si el corte ya existente no es "Vacuno", cuarto viene null del backend — el
    // formulario igual necesita un valor de arranque por si el dueño vuelve a "Vacuno".
    cuarto: corteExistente.cuarto ?? 'Ambos',
    tipoProducto: corteExistente.tipoProducto,
    zonaMapa: corteExistente.zonaMapa ?? '',
    activo: corteExistente.activo,
    precioVenta: corteExistente.precioVenta ?? '',
  }
}

interface FormularioCorteProps {
  /** `null` = alta; un corte = edición (precarga sus valores). */
  corteExistente: Corte | null
  onGuardar: (valores: CorteFormValues) => void
  onCancelar: () => void
  guardando: boolean
}

/** FR-109/FR-501: alta y edición de un corte, mismo formulario para ambos casos. */
export function FormularioCorte({ corteExistente, onGuardar, onCancelar, guardando }: FormularioCorteProps) {
  const [valores, setValores] = useState<CorteFormValues>(() => valoresIniciales(corteExistente))
  const [error, setError] = useState<string | null>(null)

  function cambiar<Campo extends keyof CorteFormValues>(campo: Campo, valor: CorteFormValues[Campo]) {
    setValores((previo) => ({ ...previo, [campo]: valor }))
  }

  function confirmar(evento: FormEvent) {
    evento.preventDefault()
    if (valores.nombre.trim() === '') {
      setError('El nombre no puede quedar vacío.')
      return
    }
    if (valores.plu.trim() === '') {
      setError('El PLU no puede quedar vacío.')
      return
    }
    setError(null)
    onGuardar(valores)
  }

  return (
    <form onSubmit={confirmar} className="flex flex-col gap-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div className="flex flex-col gap-1">
          <label htmlFor="corte-nombre" className="text-etiqueta font-medium uppercase text-texto-secundario">
            Nombre
          </label>
          <input
            id="corte-nombre"
            type="text"
            value={valores.nombre}
            onChange={(e) => cambiar('nombre', e.target.value)}
            className="h-11 rounded-xl border border-borde-campo px-3 text-cuerpo"
          />
        </div>

        <div className="flex flex-col gap-1">
          <label htmlFor="corte-plu" className="text-etiqueta font-medium uppercase text-texto-secundario">
            PLU
          </label>
          <input
            id="corte-plu"
            type="text"
            inputMode="numeric"
            value={valores.plu}
            onChange={(e) => cambiar('plu', e.target.value)}
            className="numero h-11 rounded-xl border border-borde-campo px-3"
          />
        </div>

        <div className="flex flex-col gap-1">
          <label htmlFor="corte-tipo-producto" className="text-etiqueta font-medium uppercase text-texto-secundario">
            Tipo de producto
          </label>
          <select
            id="corte-tipo-producto"
            value={valores.tipoProducto}
            onChange={(e) => cambiar('tipoProducto', e.target.value as CorteFormValues['tipoProducto'])}
            className="h-11 rounded-xl border border-borde-campo px-3 text-cuerpo"
          >
            {TIPOS_DE_PRODUCTO.map(({ valor, etiqueta }) => (
              <option key={valor} value={valor}>
                {etiqueta}
              </option>
            ))}
          </select>
        </div>

        {valores.tipoProducto === 'Vacuno' && (
          <div className="flex flex-col gap-1">
            <label htmlFor="corte-cuarto" className="text-etiqueta font-medium uppercase text-texto-secundario">
              Cuarto
            </label>
            <select
              id="corte-cuarto"
              value={valores.cuarto}
              onChange={(e) => cambiar('cuarto', e.target.value as CorteFormValues['cuarto'])}
              className="h-11 rounded-xl border border-borde-campo px-3 text-cuerpo"
            >
              {CUARTOS.map((cuarto) => (
                <option key={cuarto} value={cuarto}>
                  {cuarto}
                </option>
              ))}
            </select>
          </div>
        )}

        <div className="flex flex-col gap-1">
          <label htmlFor="corte-zona-mapa" className="text-etiqueta font-medium uppercase text-texto-secundario">
            Zona de mapa (opcional)
          </label>
          <input
            id="corte-zona-mapa"
            type="text"
            value={valores.zonaMapa}
            onChange={(e) => cambiar('zonaMapa', e.target.value)}
            className="h-11 rounded-xl border border-borde-campo px-3 text-cuerpo"
          />
        </div>

        <div className="flex flex-col gap-1">
          <label htmlFor="corte-precio-venta" className="text-etiqueta font-medium uppercase text-texto-secundario">
            Precio de venta ($/kg, opcional)
          </label>
          <input
            id="corte-precio-venta"
            type="text"
            inputMode="decimal"
            value={valores.precioVenta}
            onChange={(e) => cambiar('precioVenta', e.target.value)}
            className="numero h-11 rounded-xl border border-borde-campo px-3"
          />
        </div>

        <div className="flex items-end gap-2 pb-2">
          <input
            id="corte-activo"
            type="checkbox"
            checked={valores.activo}
            onChange={(e) => cambiar('activo', e.target.checked)}
            className="h-5 w-5"
          />
          <label htmlFor="corte-activo" className="text-cuerpo text-texto">
            Activo
          </label>
        </div>
      </div>

      {error && <p className="text-cuerpo text-error">{error}</p>}

      <div className="flex gap-3">
        <button
          type="button"
          onClick={onCancelar}
          className="h-11 rounded-xl border border-borde-campo px-4 font-medium text-texto"
        >
          Cancelar
        </button>
        <button
          type="submit"
          disabled={guardando}
          className="h-11 rounded-xl bg-vendible px-4 font-medium text-white disabled:opacity-60"
        >
          {guardando ? 'Guardando…' : 'Guardar'}
        </button>
      </div>
    </form>
  )
}
