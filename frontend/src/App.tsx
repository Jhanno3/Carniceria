import { useEffect, useState } from 'react'
import type { Session } from '@supabase/supabase-js'
import { AjustesPage } from './features/ajustes/AjustesPage'
import { AuthPage } from './features/auth/AuthPage'
import { ControlDiarioPage } from './features/control-diario/ControlDiarioPage'
import { DespostadoPage } from './features/despostado/DespostadoPage'
import { InicioPage } from './features/inicio/InicioPage'
import { usePerfilPropio } from './features/perfiles/api/usePerfilPropio'
import { UsuariosPage } from './features/perfiles/UsuariosPage'
import { ReportesPage } from './features/reportes/ReportesPage'
import { supabase } from './shared/supabase/cliente'

type Seccion = 'inicio' | 'despostado' | 'control-diario' | 'ajustes' | 'reportes' | 'usuarios'

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

function NavOperativa({
  mostrarInicioYDespostado,
  mostrarAjustes,
  mostrarReportes,
  mostrarUsuarios,
  seccion,
  onCambiarSeccion,
}: {
  mostrarInicioYDespostado: boolean
  mostrarAjustes: boolean
  mostrarReportes: boolean
  mostrarUsuarios: boolean
  seccion: Seccion
  onCambiarSeccion: (s: Seccion) => void
}) {
  function pastilla(id: Seccion, etiqueta: string) {
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
        {mostrarInicioYDespostado && pastilla('inicio', 'Inicio')}
        {mostrarInicioYDespostado && pastilla('despostado', 'Despostado')}
        {pastilla('control-diario', 'Control diario')}
        {mostrarAjustes && pastilla('ajustes', 'Ajustes')}
        {mostrarReportes && pastilla('reportes', 'Reportes')}
        {mostrarUsuarios && pastilla('usuarios', 'Usuarios')}
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
  const [seccion, setSeccion] = useState<Seccion>('inicio')

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
    return <EstadoDeCuenta mensaje="Tu cuenta está pendiente de aprobación. Avisale al administrador." />
  }

  if (perfil.estado === 'rechazado') {
    return <EstadoDeCuenta mensaje="Tu solicitud fue rechazada. Consultá con el administrador." />
  }

  if (perfil.estado === 'pausado') {
    return <EstadoDeCuenta mensaje="Tu cuenta fue pausada. Consultá con el administrador." />
  }

  const esAdmin = perfil.rol === 'admin'
  // "Dueño de sí mismo" (duenoId === su propio id), no "rol === dueno" a secas: cubre
  // tanto a un dueño real como a un admin que también opera su propio negocio de prueba
  // (ver InvitarEmpleado/InicioPage) — ambos ven Inicio/Despostado/Control diario/Ajustes.
  const operaNegocio = perfil.duenoId === perfil.id
  const esEmpleado = perfil.rol === 'empleado'

  if (esAdmin || operaNegocio || esEmpleado) {
    // Qué secciones puede ver esta cuenta, y a cuál cae por defecto si `seccion` (el
    // estado, que arranca en 'inicio') no es una de las suyas — ej. un admin puro
    // (sin negocio propio) nunca ve "inicio", así que no puede quedar colgado ahí.
    const seccionesDisponibles: Seccion[] = [
      ...(operaNegocio ? (['inicio', 'despostado'] as const) : []),
      'control-diario',
      ...(operaNegocio ? (['ajustes', 'reportes'] as const) : []),
      ...(esAdmin ? (['usuarios'] as const) : []),
    ]
    const porDefecto: Seccion = operaNegocio ? 'inicio' : esEmpleado ? 'control-diario' : 'usuarios'
    const seccionEfectiva = seccionesDisponibles.includes(seccion) ? seccion : porDefecto

    return (
      <>
        <NavOperativa
          mostrarInicioYDespostado={operaNegocio}
          mostrarAjustes={operaNegocio}
          mostrarReportes={operaNegocio}
          mostrarUsuarios={esAdmin}
          seccion={seccionEfectiva}
          onCambiarSeccion={setSeccion}
        />
        {seccionEfectiva === 'inicio' && <InicioPage />}
        {seccionEfectiva === 'despostado' && <DespostadoPage onIrAInicio={() => setSeccion('inicio')} />}
        {seccionEfectiva === 'control-diario' && <ControlDiarioPage />}
        {seccionEfectiva === 'ajustes' && <AjustesPage />}
        {seccionEfectiva === 'reportes' && <ReportesPage />}
        {seccionEfectiva === 'usuarios' && <UsuariosPage />}
      </>
    )
  }

  // No admin, no dueño de ningún negocio, no empleado de nadie: estado inconsistente
  // (ej. un "dueno" sin dueno_id todavía, entre que lo aprueban y que carga su catálogo).
  return <EstadoDeCuenta mensaje="Tu cuenta está aprobada, pero todavía no tiene un negocio asociado." />
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
