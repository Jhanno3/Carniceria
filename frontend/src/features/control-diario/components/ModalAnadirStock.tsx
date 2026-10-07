import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Modal } from '../../../shared/ui/Modal'
import { ConfirmacionExito } from '../../../shared/ui/ConfirmacionExito'
import { useCortes } from '../../../shared/api/useCortes'
import { useAnadirStock } from '../api/useAnadirStock'
import { parsearNumero } from '../../../shared/formato/formatoEsAr'
import { ApiError } from '../../../shared/api/apiFetch'
import type { CorteResponse } from '../../../shared/api/types'

type CategoriaNoVacuno = Exclude<CorteResponse['tipoProducto'], 'Vacuno'>

// Orden fijo (feedback del dueño tras probar el modal): antes de elegir el corte, elegir
// a qué categoría pertenece — con los ~20 cortes de las tres categorías juntos en un solo
// select plano, quedaba ilegible.
const CATEGORIAS: { valor: CategoriaNoVacuno; etiqueta: string }[] = [
  { valor: 'AchurasEmbutidos', etiqueta: 'Achuras y Embutidos' },
  { valor: 'Cerdo', etiqueta: 'Cerdo' },
  { valor: 'Carne', etiqueta: 'Carne' },
]

const MS_CONFIRMACION = 1100

interface ModalAnadirStockProps {
  onCerrar: () => void
}

/** FR-703: alta rápida de stock para cortes que no se despostan de una media res
 * (Achuras/Embutidos, Cerdo, Carne — ej. Rabo/Carne picada), sin pasar por Despostado. */
export function ModalAnadirStock({ onCerrar }: ModalAnadirStockProps) {
  const { data: cortes } = useCortes()
  const anadirStock = useAnadirStock()
  const [categoria, setCategoria] = useState<CategoriaNoVacuno | ''>('')
  const [corteId, setCorteId] = useState('')
  const [kgTexto, setKgTexto] = useState('')
  const [precioTexto, setPrecioTexto] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [guardadoOk, setGuardadoOk] = useState(false)

  // Confirmación breve (tilde + texto) antes de cerrar solo, para que quede claro que el
  // stock se cargó — pedido del dueño tras probar el modal, antes cerraba sin avisar nada.
  useEffect(() => {
    if (!guardadoOk) return
    const id = setTimeout(onCerrar, MS_CONFIRMACION)
    return () => clearTimeout(id)
  }, [guardadoOk, onCerrar])

  const cortesDisponibles = (cortes ?? []).filter((c) => c.activo && c.tipoProducto !== 'Vacuno')
  const sinCortesDisponibles = cortes !== undefined && cortesDisponibles.length === 0
  const cortesDeLaCategoria = categoria === '' ? [] : cortesDisponibles.filter((c) => c.tipoProducto === categoria)

  function cambiarCategoria(valor: CategoriaNoVacuno | '') {
    setCategoria(valor)
    setCorteId('') // el corte elegido antes puede no pertenecer a la nueva categoría
  }

  async function confirmar(evento: FormEvent) {
    evento.preventDefault()
    const corte = cortesDeLaCategoria.find((c) => c.id === corteId)
    if (!corte) {
      setError('Elegí una categoría y un corte.')
      return
    }
    let kg: number
    try {
      kg = parsearNumero(kgTexto)
    } catch {
      setError('Los kilos no son un número válido.')
      return
    }
    if (kg <= 0) {
      setError('Los kilos tienen que ser mayores a 0.')
      return
    }

    setError(null)
    try {
      await anadirStock.mutateAsync({
        corteId: corte.id,
        kg: String(kg),
        precioKg: precioTexto.trim() === '' ? null : String(parsearNumero(precioTexto)),
        tipoEntrada: corte.tipoProducto as CategoriaNoVacuno,
      })
      setGuardadoOk(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos guardar el stock.')
    }
  }

  if (guardadoOk) {
    return (
      <Modal titulo="Añadir stock" onCerrar={onCerrar}>
        <ConfirmacionExito mensaje="Stock añadido" />
      </Modal>
    )
  }

  return (
    <Modal titulo="Añadir stock" onCerrar={onCerrar}>
      <form onSubmit={confirmar} className="flex flex-col gap-4">
        {sinCortesDisponibles ? (
          <p className="text-cuerpo text-texto-secundario">
            No tenés cortes de Achuras/Embutidos, Cerdo o Carne activos. Cargalos en Editar cortes.
          </p>
        ) : (
          <>
            <div className="flex flex-col gap-1">
              <label htmlFor="stock-categoria" className="text-etiqueta font-medium uppercase text-texto-secundario">
                Tipo de corte
              </label>
              <select
                id="stock-categoria"
                value={categoria}
                onChange={(e) => cambiarCategoria(e.target.value as CategoriaNoVacuno | '')}
                className="h-11 rounded-xl border border-borde-campo px-3 text-cuerpo"
              >
                <option value="">Elegí un tipo</option>
                {CATEGORIAS.map(({ valor, etiqueta }) => (
                  <option key={valor} value={valor}>
                    {etiqueta}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex flex-col gap-1">
              <label htmlFor="stock-corte" className="text-etiqueta font-medium uppercase text-texto-secundario">
                Corte
              </label>
              <select
                id="stock-corte"
                value={corteId}
                onChange={(e) => setCorteId(e.target.value)}
                disabled={categoria === ''}
                className="h-11 rounded-xl border border-borde-campo px-3 text-cuerpo disabled:bg-fondo-suave disabled:text-texto-secundario"
              >
                <option value="">{categoria === '' ? 'Elegí primero un tipo' : 'Elegí un corte'}</option>
                {cortesDeLaCategoria.map((corte) => (
                  <option key={corte.id} value={corte.id}>
                    {corte.nombre}
                  </option>
                ))}
              </select>
              {categoria !== '' && cortesDeLaCategoria.length === 0 && (
                <p className="text-etiqueta text-texto-secundario">No hay cortes activos en esta categoría.</p>
              )}
            </div>

            <div className="flex flex-col gap-1">
              <label htmlFor="stock-kg" className="text-etiqueta font-medium uppercase text-texto-secundario">
                Kilos
              </label>
              <input
                id="stock-kg"
                type="text"
                inputMode="decimal"
                value={kgTexto}
                onChange={(e) => setKgTexto(e.target.value)}
                className="numero h-11 rounded-xl border border-borde-campo px-3"
              />
            </div>

            <div className="flex flex-col gap-1">
              <label htmlFor="stock-precio" className="text-etiqueta font-medium uppercase text-texto-secundario">
                Precio de compra ($/kg, opcional)
              </label>
              <input
                id="stock-precio"
                type="text"
                inputMode="decimal"
                value={precioTexto}
                onChange={(e) => setPrecioTexto(e.target.value)}
                className="numero h-11 rounded-xl border border-borde-campo px-3"
              />
            </div>
          </>
        )}

        {error && <p className="text-cuerpo text-error">{error}</p>}

        <div className="flex gap-3">
          <button
            type="button"
            onClick={onCerrar}
            className="h-11 rounded-xl border border-borde-campo px-4 font-medium text-texto"
          >
            Cancelar
          </button>
          <button
            type="submit"
            disabled={sinCortesDisponibles || !corteId || kgTexto.trim() === '' || anadirStock.isPending}
            className="h-11 rounded-xl bg-vendible px-4 font-medium text-white disabled:opacity-60"
          >
            {anadirStock.isPending ? 'Guardando…' : 'Guardar'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
