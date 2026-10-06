import { useState } from 'react'

interface SelectorDeRangoProps {
  idPrefix: string
  desde: string
  hasta: string
  onAplicar: (desde: string, hasta: string) => void
}

/** Dos campos de fecha + "Aplicar" — reutilizado por los 3 reportes (plan-fase4.md). */
export function SelectorDeRango({ idPrefix, desde, hasta, onAplicar }: SelectorDeRangoProps) {
  const [desdeBorrador, setDesdeBorrador] = useState(desde)
  const [hastaBorrador, setHastaBorrador] = useState(hasta)

  return (
    <div className="flex flex-wrap items-end gap-3">
      <div className="flex flex-col gap-1">
        <label
          htmlFor={`${idPrefix}-desde`}
          className="text-etiqueta font-medium uppercase text-texto-secundario"
        >
          Desde
        </label>
        <input
          id={`${idPrefix}-desde`}
          type="date"
          value={desdeBorrador}
          onChange={(e) => setDesdeBorrador(e.target.value)}
          className="h-11 rounded-xl border border-borde-campo px-3 text-cuerpo"
        />
      </div>
      <div className="flex flex-col gap-1">
        <label
          htmlFor={`${idPrefix}-hasta`}
          className="text-etiqueta font-medium uppercase text-texto-secundario"
        >
          Hasta
        </label>
        <input
          id={`${idPrefix}-hasta`}
          type="date"
          value={hastaBorrador}
          onChange={(e) => setHastaBorrador(e.target.value)}
          className="h-11 rounded-xl border border-borde-campo px-3 text-cuerpo"
        />
      </div>
      <button
        type="button"
        onClick={() => onAplicar(desdeBorrador, hastaBorrador)}
        className="h-11 rounded-xl bg-vendible px-4 font-medium text-white"
      >
        Aplicar
      </button>
    </div>
  )
}
