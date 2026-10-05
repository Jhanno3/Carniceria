import { useColaPendiente } from '../api/useColaPendiente'

/** FR-303: la pantalla muestra en todo momento cuántas ventas están pendientes de subir. */
export function IndicadorPendientes() {
  const { data: cantidad = 0 } = useColaPendiente()

  if (cantidad === 0) {
    return <p className="text-cuerpo text-texto-secundario">Sin pendientes de sincronizar.</p>
  }

  return (
    <div className="rounded-xl border-l-4 border-l-aviso-texto bg-aviso-fondo p-3 text-cuerpo text-aviso-texto">
      {cantidad} {cantidad === 1 ? 'venta pendiente de subir' : 'ventas pendientes de subir'}
    </div>
  )
}
