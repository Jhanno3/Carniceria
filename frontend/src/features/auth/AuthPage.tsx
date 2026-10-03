import { useState } from 'react'
import { LoginForm } from './LoginForm'
import { RegisterForm } from './RegisterForm'

export function AuthPage() {
  const [modo, setModo] = useState<'login' | 'registro'>('login')

  return (
    <main className="flex min-h-screen items-center justify-center bg-fondo-suave-2 p-4">
      <div className="w-full max-w-sm rounded-xl border border-borde bg-fondo p-4">
        {modo === 'login' ? (
          <LoginForm onCambiarARegistro={() => setModo('registro')} />
        ) : (
          <RegisterForm onCambiarALogin={() => setModo('login')} />
        )}
      </div>
    </main>
  )
}
