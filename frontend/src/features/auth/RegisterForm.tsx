import { useState } from 'react'
import type { FormEvent } from 'react'
import { supabase } from '../../shared/supabase/cliente'

export function RegisterForm({ onCambiarALogin }: { onCambiarALogin: () => void }) {
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [rol, setRol] = useState<'empleado' | 'dueno'>('empleado')
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
      options: { data: { nombre, rol_solicitado: rol } },
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
          Te mandamos un email para confirmar la cuenta. Una vez que la confirmes, alguien con acceso de
          dueño tiene que aprobarte — hasta entonces no vas a poder entrar.
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
      <p className="text-cuerpo text-texto-secundario">
        Cualquiera puede pedir una cuenta, pero nadie entra hasta que el dueño lo apruebe.
      </p>

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

      <fieldset className="flex flex-col gap-2">
        <legend className="text-etiqueta font-medium uppercase text-texto-secundario">Pedís entrar como</legend>
        <label className="flex h-11 items-center gap-2 text-cuerpo text-texto">
          <input type="radio" name="rol" value="empleado" checked={rol === 'empleado'} onChange={() => setRol('empleado')} />
          Empleado
        </label>
        <label className="flex h-11 items-center gap-2 text-cuerpo text-texto">
          <input type="radio" name="rol" value="dueno" checked={rol === 'dueno'} onChange={() => setRol('dueno')} />
          Dueño
        </label>
      </fieldset>

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
