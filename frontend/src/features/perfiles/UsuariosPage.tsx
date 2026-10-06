import { usePerfilesTodos } from './api/usePerfilesTodos'
import { useActualizarPerfil } from './api/useActualizarPerfil'
import { usePerfilPropio } from './api/usePerfilPropio'
import type { PerfilResponse } from './api/types'

export function UsuariosPage() {
  const { data: todas, isLoading } = usePerfilesTodos(true)
  const { data: perfilPropio } = usePerfilPropio(true)
  const actualizar = useActualizarPerfil()

  const pendientes = todas?.filter((p) => p.estado === 'pendiente') ?? []

  function pausar(perfil: PerfilResponse) {
    actualizar.mutate({ id: perfil.id, rol: perfil.rol, estado: 'pausado' })
  }

  function reactivar(perfil: PerfilResponse) {
    actualizar.mutate({ id: perfil.id, rol: perfil.rol, estado: 'aprobado' })
  }

  return (
    <main className="mx-auto flex max-w-2xl flex-col gap-8 p-4">
      <div>
        <h1 className="mb-4 text-titulo-seccion font-titulos font-bold text-texto">Usuarios</h1>

        {isLoading && <p className="text-cuerpo text-texto-secundario">Cargando…</p>}

        {todas && pendientes.length === 0 && (
          <p className="text-cuerpo text-texto-secundario">No hay cuentas pendientes de aprobación.</p>
        )}

        <ul className="flex flex-col gap-3">
          {pendientes.map((perfil) => (
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
      </div>

      <div className="flex flex-col gap-3">
        <h2 className="text-titulo-seccion font-titulos font-bold text-texto">Todas las cuentas</h2>
        <div className="overflow-x-auto rounded-xl border border-borde">
          <table className="w-full text-cuerpo">
            <thead>
              <tr className="border-b border-borde text-etiqueta uppercase text-texto-secundario">
                <th className="p-3 text-left">Nombre</th>
                <th className="p-3 text-left">Rol</th>
                <th className="p-3 text-left">Estado</th>
                <th className="p-3 text-left"></th>
              </tr>
            </thead>
            <tbody>
              {(todas ?? []).map((perfil) => (
                <tr key={perfil.id} className="border-b border-borde last:border-0">
                  <td className="p-2 font-medium text-texto">{perfil.nombre ?? '(sin nombre)'}</td>
                  <td className="p-2 text-texto-secundario">{perfil.rol}</td>
                  <td className="p-2 text-texto-secundario">{perfil.estado}</td>
                  <td className="p-2 text-right">
                    {perfil.id === perfilPropio?.id ? null : perfil.estado === 'aprobado' ? (
                      <button
                        type="button"
                        disabled={actualizar.isPending}
                        onClick={() => pausar(perfil)}
                        className="h-9 rounded-lg border border-borde-campo px-3 text-etiqueta font-medium text-texto disabled:opacity-60"
                      >
                        Pausar
                      </button>
                    ) : perfil.estado === 'pausado' ? (
                      <button
                        type="button"
                        disabled={actualizar.isPending}
                        onClick={() => reactivar(perfil)}
                        className="h-9 rounded-lg bg-vendible px-3 text-etiqueta font-medium text-white disabled:opacity-60"
                      >
                        Reactivar
                      </button>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </main>
  )
}
