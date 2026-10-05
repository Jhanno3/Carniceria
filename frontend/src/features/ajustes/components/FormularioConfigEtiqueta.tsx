import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useConfigEtiqueta } from '../api/useConfigEtiqueta'
import { useActualizarConfigEtiqueta } from '../api/useActualizarConfigEtiqueta'
import { ApiError } from '../../../shared/api/apiFetch'
import type { ConfigEtiquetaDto } from '../api/types'

const CAMPOS_NUMERICOS: { campo: keyof Omit<ConfigEtiquetaDto, 'tipoValor'>; etiqueta: string }[] = [
  { campo: 'prefijoDesde', etiqueta: 'Prefijo desde' },
  { campo: 'prefijoHasta', etiqueta: 'Prefijo hasta' },
  { campo: 'inicioPlu', etiqueta: 'Inicio del PLU' },
  { campo: 'largoPlu', etiqueta: 'Largo del PLU' },
  { campo: 'inicioValor', etiqueta: 'Inicio del valor' },
  { campo: 'largoValor', etiqueta: 'Largo del valor' },
  { campo: 'decimales', etiqueta: 'Decimales' },
]

/** FR-209: pantalla de ajustes para que el dueño edite config_etiqueta sin tocar código. */
export function FormularioConfigEtiqueta() {
  const { data: config, isLoading } = useConfigEtiqueta()
  const actualizar = useActualizarConfigEtiqueta()
  const [valores, setValores] = useState<ConfigEtiquetaDto | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [guardadoOk, setGuardadoOk] = useState(false)

  useEffect(() => {
    if (config) setValores(config)
  }, [config])

  if (isLoading || !valores) {
    return <p className="text-cuerpo text-texto-secundario">Cargando…</p>
  }

  function cambiarNumero(campo: keyof ConfigEtiquetaDto, texto: string) {
    const numero = Number(texto)
    setValores((previo) => (previo ? { ...previo, [campo]: Number.isNaN(numero) ? 0 : numero } : previo))
  }

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    if (!valores) return
    setError(null)
    setGuardadoOk(false)
    try {
      await actualizar.mutateAsync(valores)
      setGuardadoOk(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos guardar los cambios.')
    }
  }

  return (
    <form onSubmit={guardar} className="flex flex-col gap-4">
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
        {CAMPOS_NUMERICOS.map(({ campo, etiqueta }) => (
          <div key={campo} className="flex flex-col gap-1">
            <label htmlFor={`config-${campo}`} className="text-etiqueta font-medium uppercase text-texto-secundario">
              {etiqueta}
            </label>
            <input
              id={`config-${campo}`}
              type="number"
              value={valores[campo]}
              onChange={(e) => cambiarNumero(campo, e.target.value)}
              className="numero h-11 rounded-xl border border-borde-campo px-3"
            />
          </div>
        ))}

        <div className="flex flex-col gap-1">
          <label htmlFor="config-tipoValor" className="text-etiqueta font-medium uppercase text-texto-secundario">
            Tipo de valor
          </label>
          <select
            id="config-tipoValor"
            value={valores.tipoValor}
            onChange={(e) =>
              setValores((previo) => (previo ? { ...previo, tipoValor: e.target.value as 'peso' | 'importe' } : previo))
            }
            className="h-11 rounded-xl border border-borde-campo px-3"
          >
            <option value="peso">Peso</option>
            <option value="importe">Importe</option>
          </select>
        </div>
      </div>

      {error && <p className="text-cuerpo text-error">{error}</p>}
      {guardadoOk && !error && <p className="text-cuerpo text-exito-texto">Guardado.</p>}

      <button
        type="submit"
        disabled={actualizar.isPending}
        className="h-11 w-fit rounded-xl bg-vendible px-4 font-medium text-white disabled:opacity-60"
      >
        {actualizar.isPending ? 'Guardando…' : 'Guardar'}
      </button>
    </form>
  )
}
