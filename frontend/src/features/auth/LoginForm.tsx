import { useState } from 'react'
import type { FormEvent } from 'react'
import { supabase } from '../../shared/supabase/cliente'

export function LoginForm({ onCambiarARegistro }: { onCambiarARegistro: () => void }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    const { error: errorSupabase } = await supabase.auth.signInWithPassword({ email, password })
    setEnviando(false)
    if (errorSupabase) {
      setError('No pudimos iniciar sesión. Revisá el email y la contraseña.')
    }
  }

  return (
    <form onSubmit={enviar} className="flex flex-col gap-4">
      <h1 className="text-titulo-seccion font-titulos font-bold text-texto">Iniciá sesión</h1>

      <div className="flex flex-col gap-1">
        <label htmlFor="login-email" className="text-etiqueta font-medium uppercase text-texto-secundario">
          Email
        </label>
        <input
          id="login-email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="h-11 rounded-xl border border-borde-campo px-3 text-cuerpo"
        />
      </div>

      <div className="flex flex-col gap-1">
        <label htmlFor="login-password" className="text-etiqueta font-medium uppercase text-texto-secundario">
          Contraseña
        </label>
        <input
          id="login-password"
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="h-11 rounded-xl border border-borde-campo px-3 text-cuerpo"
        />
      </div>

      {error && <p className="text-cuerpo text-error">{error}</p>}

      <button
        type="submit"
        disabled={enviando}
        className="h-11 rounded-xl bg-vendible font-medium text-white disabled:opacity-60"
      >
        {enviando ? 'Entrando…' : 'Entrar'}
      </button>

      <button type="button" onClick={onCambiarARegistro} className="h-11 text-cuerpo text-texto-secundario underline">
        ¿No tenés cuenta? Registrate
      </button>
    </form>
  )
}
