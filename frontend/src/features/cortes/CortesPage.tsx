import { useEffect, useState } from 'react'
import { useCortes } from './api/useCortes'
import { useCrearCorte } from './api/useCrearCorte'
import { useActualizarCorte } from './api/useActualizarCorte'
import { FormularioCorte } from './components/FormularioCorte'
import { TablaDeCortes } from './components/TablaDeCortes'
import { Modal } from '../../shared/ui/Modal'
import { ConfirmacionExito } from '../../shared/ui/ConfirmacionExito'
import { ApiError } from '../../shared/api/apiFetch'
import type { Corte, CorteFormValues } from './api/types'

const MS_CONFIRMACION = 1100

/** FR-109/FR-501: pantalla "Editar cortes" — alta, edición y activar/desactivar. */
export function CortesPage() {
  const { data: cortes, isLoading } = useCortes(true)
  const crear = useCrearCorte()
  const actualizar = useActualizarCorte()
  const [corteEnEdicion, setCorteEnEdicion] = useState<Corte | null | undefined>(undefined)
  const [error, setError] = useState<string | null>(null)
  const [guardadoOk, setGuardadoOk] = useState(false)

  const formularioAbierto = corteEnEdicion !== undefined
  const guardando = crear.isPending || actualizar.isPending

  // Confirmación breve antes de cerrar el modal solo (mismo criterio que "Añadir stock" de
  // Control diario, pedido por chat).
  useEffect(() => {
    if (!guardadoOk) return
    const id = setTimeout(() => {
      setGuardadoOk(false)
      setCorteEnEdicion(undefined)
    }, MS_CONFIRMACION)
    return () => clearTimeout(id)
  }, [guardadoOk])

  async function guardar(valores: CorteFormValues) {
    setError(null)
    try {
      if (corteEnEdicion) {
        await actualizar.mutateAsync({ id: corteEnEdicion.id, ...valores })
      } else {
        await crear.mutateAsync(valores)
      }
      setGuardadoOk(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos guardar los cambios.')
    }
  }

  async function cambiarActivo(corte: Corte) {
    setError(null)
    try {
      await actualizar.mutateAsync({
        id: corte.id,
        nombre: corte.nombre,
        plu: String(corte.plu),
        cuarto: corte.cuarto ?? 'Ambos',
        tipoProducto: corte.tipoProducto,
        zonaMapa: corte.zonaMapa ?? '',
        activo: !corte.activo,
        precioVenta: corte.precioVenta ?? '',
        descuentaStockDeCorteId: corte.descuentaStockDeCorteId ?? '',
      })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos guardar los cambios.')
    }
  }

  return (
    <main className="mx-auto flex max-w-4xl flex-col gap-4 p-4">
      <div className="flex items-center justify-between gap-2">
        <h1 className="text-titulo-seccion font-titulos font-bold text-texto">Editar cortes</h1>
        <button
          type="button"
          onClick={() => setCorteEnEdicion(null)}
          className="h-11 rounded-xl bg-vendible px-4 font-medium text-white"
        >
          Nuevo corte
        </button>
      </div>

      {error && <p className="text-cuerpo text-error">{error}</p>}

      {isLoading && <p className="text-cuerpo text-texto-secundario">Cargando…</p>}

      {cortes && (
        <TablaDeCortes
          cortes={cortes}
          onEditar={setCorteEnEdicion}
          onCambiarActivo={cambiarActivo}
          guardando={guardando}
        />
      )}

      {formularioAbierto && (
        <Modal
          titulo={corteEnEdicion ? 'Editar corte' : 'Nuevo corte'}
          onCerrar={() => setCorteEnEdicion(undefined)}
        >
          {guardadoOk ? (
            <ConfirmacionExito mensaje={corteEnEdicion ? 'Corte editado' : 'Corte creado'} />
          ) : (
            <FormularioCorte
              corteExistente={corteEnEdicion ?? null}
              cortes={cortes ?? []}
              onGuardar={guardar}
              onCancelar={() => setCorteEnEdicion(undefined)}
              guardando={guardando}
            />
          )}
        </Modal>
      )}
    </main>
  )
}
