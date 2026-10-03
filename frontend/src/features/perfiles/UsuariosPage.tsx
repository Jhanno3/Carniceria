import { usePerfilesPendientes } from './api/usePerfilesPendientes'
import { useActualizarPerfil } from './api/useActualizarPerfil'

export function UsuariosPage() {
  const { data: pendientes, isLoading } = usePerfilesPendientes(true)
  const actualizar = useActualizarPerfil()

  return (
    <main className="mx-auto max-w-2xl p-4">
      <h1 className="mb-4 text-titulo-seccion font-titulos font-bold text-texto">Usuarios</h1>

      {isLoading && <p className="text-cuerpo text-texto-secundario">Cargando…</p>}

      {pendientes && pendientes.length === 0 && (
        <p className="text-cuerpo text-texto-secundario">No hay cuentas pendientes de aprobación.</p>
      )}

      <ul className="flex flex-col gap-3">
        {pendientes?.map((perfil) => (
          <li
            key={perfil.id}
            className="flex items-center justify-between rounded-xl border border-borde p-4"
          >
            <div>
              <p className="text-cuerpo font-medium text-texto">{perfil.nombre ?? '(sin nombre)'}</p>
              <p className="text-etiqueta uppercase text-texto-secundario">Pide ser: {perfil.rol}</p>
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                disabled={actualizar.isPending}
                onClick={() => actualizar.mutate({ id: perfil.id, rol: perfil.rol, estado: 'aprobado' })}
                className="h-11 rounded-xl bg-vendible px-4 font-medium text-white disabled:opacity-60"
              >
                Aprobar
              </button>
              <button
                type="button"
                disabled={actualizar.isPending}
                onClick={() => actualizar.mutate({ id: perfil.id, rol: perfil.rol, estado: 'rechazado' })}
                className="h-11 rounded-xl border border-borde-campo px-4 font-medium text-texto disabled:opacity-60"
              >
                Rechazar
              </button>
            </div>
          </li>
        ))}
      </ul>
    </main>
  )
}
