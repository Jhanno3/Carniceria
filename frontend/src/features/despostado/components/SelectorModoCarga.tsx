interface SelectorModoCargaProps {
  disponibleAutomatico: boolean
  onElegir: (modo: 'manual' | 'automatico') => void
}

export function SelectorModoCarga({ disponibleAutomatico, onElegir }: SelectorModoCargaProps) {
  return (
    <div className="flex flex-col gap-3">
      <h2 className="text-titulo-seccion font-titulos font-bold text-texto">¿Cómo cargás esta entrada?</h2>
      <div className="flex flex-col gap-3 sm:flex-row">
        <button
          type="button"
          onClick={() => onElegir('manual')}
          className="h-11 flex-1 rounded-xl bg-vendible px-4 font-medium text-white"
        >
          Manual — cargo cada corte a mano
        </button>
        <button
          type="button"
          disabled={!disponibleAutomatico}
          onClick={() => onElegir('automatico')}
          className="h-11 flex-1 rounded-xl border border-borde-campo px-4 font-medium text-texto disabled:cursor-not-allowed disabled:opacity-50"
        >
          Automático — estimar con el historial
        </button>
      </div>
      {!disponibleAutomatico && (
        <p className="text-etiqueta text-texto-secundario">
          Automático no está disponible todavía: hace falta al menos una entrada cargada antes.
        </p>
      )}
    </div>
  )
}
