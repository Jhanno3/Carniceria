import { useState } from 'react'
import type { FormEvent } from 'react'
import { supabase } from '../../shared/supabase/cliente'

interface RegisterFormProps {
  onCambiarALogin: () => void
  /** Presente cuando se llegó con el link de invitación de un dueño (ver AuthPage). */
  invitaDuenoId: string | null
}

export function RegisterForm({ onCambiarALogin, invitaDuenoId }: RegisterFormProps) {
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [registrado, setRegistrado] = useState(false)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    const { error: errorSupabase } = await supabase.auth.signUp({
      email,
      password,
      options: {
        data: invitaDuenoId
          ? { nombre, rol_solicitado: 'empleado', dueno_invitador_id: invitaDuenoId }
          : { nombre, rol_solicitado: 'dueno' },
      },
    })
    setEnviando(false)
    if (errorSupabase) {
      setError('No pudimos crear la cuenta. Probá de nuevo en un rato.')
      return
    }
    setRegistrado(true)
  }

  if (registrado) {
    return (
      <div className="flex flex-col gap-3">
        <h1 className="text-titulo-seccion font-titulos font-bold text-texto">Listo, casi</h1>
        <p className="text-cuerpo text-texto">
          {invitaDuenoId
            ? 'Te mandamos un email para confirmar la cuenta. En cuanto la confirmes, ya podés entrar.'
            : 'Te mandamos un email para confirmar la cuenta. Una vez que la confirmes, el administrador tiene que aprobarte — hasta entonces no vas a poder entrar.'}
        </p>
        <button type="button" onClick={onCambiarALogin} className="h-11 text-cuerpo text-texto-secundario underline">
          Volver a iniciar sesión
        </button>
      </div>
    )
  }

  return (
    <form onSubmit={enviar} className="flex flex-col gap-4">
      <h1 className="text-titulo-seccion font-titulos font-bold text-texto">Registrate</h1>
      {invitaDuenoId ? (
        <p className="text-cuerpo text-texto-secundario">Te invitaron a sumarte como empleado.</p>
      ) : (
        <p className="text-cuerpo text-texto-secundario">
          Te registrás como dueño de tu propio negocio; nadie entra hasta que el administrador lo apruebe. ¿Sos
          empleado? Pedile el link de invitación a tu dueño en vez de registrarte acá.
        </p>
      )}

      <div className="flex flex-col gap-1">
        <label htmlFor="registro-nombre" className="text-etiqueta font-medium uppercase text-texto-secundario">
          Nombre
        </label>
        <input
          id="registro-nombre"
          type="text"
          required
          value={nombre}
          onChange={(e) => setNombre(e.target.value)}
          className="h-11 rounded-xl border border-borde-campo px-3 text-cuerpo"
        />
      </div>

      <div className="flex flex-col gap-1">
        <label htmlFor="registro-email" className="text-etiqueta font-medium uppercase text-texto-secundario">
          Email
        </label>
        <input
          id="registro-email"
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="h-11 rounded-xl border border-borde-campo px-3 text-cuerpo"
        />
      </div>

      <div className="flex flex-col gap-1">
        <label htmlFor="registro-password" className="text-etiqueta font-medium uppercase text-texto-secundario">
          Contraseña
        </label>
        <input
          id="registro-password"
          type="password"
          required
          minLength={6}
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
        {enviando ? 'Enviando…' : 'Pedir cuenta'}
      </button>

      <button type="button" onClick={onCambiarALogin} className="h-11 text-cuerpo text-texto-secundario underline">
        ¿Ya tenés cuenta? Iniciá sesión
      </button>
    </form>
  )
}
