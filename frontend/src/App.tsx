import { useEffect, useState } from 'react'
import type { Session } from '@supabase/supabase-js'
import { AuthPage } from './features/auth/AuthPage'
import { DespostadoPage } from './features/despostado/DespostadoPage'
import { usePerfilPropio } from './features/perfiles/api/usePerfilPropio'
import { UsuariosPage } from './features/perfiles/UsuariosPage'
import { supabase } from './shared/supabase/cliente'

function CerrarSesion() {
  return (
    <button
      type="button"
      onClick={() => supabase.auth.signOut()}
      className="fixed right-4 top-4 h-11 rounded-xl border border-borde-campo px-4 text-cuerpo text-texto"
    >
      Cerrar sesión
    </button>
  )
}

function EstadoDeCuenta({ mensaje }: { mensaje: string }) {
  return (
    <main className="flex min-h-screen items-center justify-center bg-fondo-suave-2 p-4">
      <p className="max-w-sm text-center text-cuerpo text-texto">{mensaje}</p>
      <CerrarSesion />
    </main>
  )
}

function NavDueno({
  seccion,
  onCambiarSeccion,
}: {
  seccion: 'despostado' | 'usuarios'
  onCambiarSeccion: (s: 'despostado' | 'usuarios') => void
}) {
  function pastilla(id: 'despostado' | 'usuarios', etiqueta: string) {
    const activa = seccion === id
    return (
      <button
        type="button"
        onClick={() => onCambiarSeccion(id)}
        className={`h-11 rounded-full px-4 font-medium ${activa ? 'bg-texto text-white' : 'text-texto'}`}
      >
        {etiqueta}
      </button>
    )
  }

  return (
    <nav className="flex items-center justify-between border-b border-borde p-4">
      <div className="flex gap-2">
        {pastilla('despostado', 'Despostado')}
        {pastilla('usuarios', 'Usuarios')}
      </div>
      <button
        type="button"
        onClick={() => supabase.auth.signOut()}
        className="h-11 rounded-xl border border-borde-campo px-4 text-cuerpo text-texto"
      >
        Cerrar sesión
      </button>
    </nav>
  )
}

function AppAutenticada() {
  const { data: perfil, isLoading, isError, error } = usePerfilPropio(true)
  const [seccion, setSeccion] = useState<'despostado' | 'usuarios'>('despostado')

  if (isError) {
    return (
      <EstadoDeCuenta
        mensaje={`No pudimos conectar con el servidor: ${error instanceof Error ? error.message : 'error desconocido'}. Revisá que el backend esté corriendo.`}
      />
    )
  }

  if (isLoading || !perfil) {
    return <EstadoDeCuenta mensaje="Cargando tu cuenta…" />
  }

  if (perfil.estado === 'pendiente') {
    return <EstadoDeCuenta mensaje="Tu cuenta está pendiente de aprobación. Avisale al dueño." />
  }

  if (perfil.estado === 'rechazado') {
    return <EstadoDeCuenta mensaje="Tu solicitud fue rechazada. Consultá con el dueño." />
  }

  if (perfil.rol === 'dueno') {
    return (
      <>
        <NavDueno seccion={seccion} onCambiarSeccion={setSeccion} />
        {seccion === 'despostado' ? <DespostadoPage /> : <UsuariosPage />}
      </>
    )
  }

  // Empleado aprobado — Control diario llega en la Fase 2.
  return <EstadoDeCuenta mensaje="Tu cuenta está aprobada. Todavía no hay nada para mostrarte acá." />
}

function App() {
  const [session, setSession] = useState<Session | null | undefined>(undefined)

  useEffect(() => {
    supabase.auth.getSession().then(({ data }) => setSession(data.session))
    const { data: suscripcion } = supabase.auth.onAuthStateChange((_evento, nuevaSesion) => {
      setSession(nuevaSesion)
    })
    return () => suscripcion.subscription.unsubscribe()
  }, [])

  if (session === undefined) {
    return <EstadoDeCuenta mensaje="Cargando…" />
  }

  return session ? <AppAutenticada /> : <AuthPage />
}

export default App
