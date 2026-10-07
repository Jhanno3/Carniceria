import { useState } from 'react'
import { BotonTema } from '../../shared/ui/BotonTema'
import { LoginForm } from './LoginForm'
import { RegisterForm } from './RegisterForm'

/** Link de invitación de un dueño (ver InvitarEmpleado): `?invita=<id-del-dueño>`. */
function leerInvitaDuenoId(): string | null {
  return new URLSearchParams(window.location.search).get('invita')
}

export function AuthPage() {
  const [invitaDuenoId] = useState(leerInvitaDuenoId)
  const [modo, setModo] = useState<'login' | 'registro'>(invitaDuenoId ? 'registro' : 'login')

  return (
    <main className="flex min-h-screen items-center justify-center bg-fondo-suave-2 p-4">
      <BotonTema className="fixed right-4 top-4" />
      <div className="w-full max-w-sm rounded-xl border border-borde bg-fondo p-4">
        {modo === 'login' ? (
          <LoginForm onCambiarARegistro={() => setModo('registro')} />
        ) : (
          <RegisterForm onCambiarALogin={() => setModo('login')} invitaDuenoId={invitaDuenoId} />
        )}
      </div>
    </main>
  )
}
