import { useState } from 'react'

export function InvitarEmpleado({ duenoId }: { duenoId: string }) {
  const [copiado, setCopiado] = useState(false)
  const link = `${window.location.origin}${window.location.pathname}?invita=${duenoId}`

  async function copiar() {
    try {
      await navigator.clipboard.writeText(link)
      setCopiado(true)
      setTimeout(() => setCopiado(false), 2000)
    } catch {
      // Clipboard puede fallar (permisos del navegador); el link ya está visible para copiar a mano.
    }
  }

  return (
    <div className="flex flex-col gap-2 rounded-xl border border-borde p-4">
      <h3 className="text-etiqueta font-medium uppercase text-texto-secundario">Invitar empleado</h3>
      <p className="text-cuerpo text-texto-secundario">
        Compartile este link a tu empleado: se registra y queda sumado a tu negocio directo, sin que nadie tenga que
        aprobarlo.
      </p>
      <div className="flex flex-col gap-2 sm:flex-row">
        <input
          readOnly
          value={link}
          onFocus={(e) => e.target.select()}
          aria-label="Link de invitación"
          className="numero h-11 flex-1 rounded-xl border border-borde-campo px-3 text-cuerpo"
        />
        <button
          type="button"
          onClick={copiar}
          className="h-11 rounded-xl bg-vendible px-4 font-medium text-white"
        >
          {copiado ? 'Copiado' : 'Copiar'}
        </button>
      </div>
    </div>
  )
}
