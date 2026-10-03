import { parsearNumero } from '../../../shared/formato/formatoEsAr'

type TipoPerdida = 'hueso' | 'grasa' | 'merma'

interface TarjetasPerdidaProps {
  pesoKg: number
  valores: Record<TipoPerdida, string>
  onCambiar: (tipo: TipoPerdida, texto: string) => void
}

const TARJETAS: { tipo: TipoPerdida; etiqueta: string; colorClase: string }[] = [
  { tipo: 'hueso', etiqueta: 'Hueso', colorClase: 'bg-hueso' },
  { tipo: 'grasa', etiqueta: 'Grasa', colorClase: 'bg-grasa' },
  { tipo: 'merma', etiqueta: 'Merma', colorClase: 'bg-merma' },
]

function aNumeroSeguro(texto: string): number {
  try {
    return parsearNumero(texto)
  } catch {
    return 0
  }
}

export function TarjetasPerdida({ pesoKg, valores, onCambiar }: TarjetasPerdidaProps) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
      {TARJETAS.map(({ tipo, etiqueta, colorClase }) => {
        const kg = aNumeroSeguro(valores[tipo])
        const porcentaje = pesoKg > 0 ? (kg / pesoKg) * 100 : 0
        return (
          <div key={tipo} className="flex flex-col gap-2 rounded-xl border border-borde p-4">
            <span className={`h-2 w-10 rounded-full ${colorClase}`} aria-hidden="true" />
            <label htmlFor={`perdida-${tipo}`} className="text-etiqueta font-medium uppercase text-texto-secundario">
              {etiqueta}
            </label>
            <input
              id={`perdida-${tipo}`}
              type="text"
              inputMode="decimal"
              value={valores[tipo]}
              onChange={(e) => onCambiar(tipo, e.target.value)}
              className="numero h-11 rounded-xl border border-borde-campo px-3"
            />
            <p className="numero text-cuerpo text-texto-secundario">
              {`${new Intl.NumberFormat('es-AR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(porcentaje)} %`}
            </p>
          </div>
        )
      })}
    </div>
  )
}
