import { formatearKg } from '../../../shared/formato/formatoEsAr'

interface BarraComposicionProps {
  pesoKg: number
  vendibleKg: number
  hueso: number
  grasa: number
  merma: number
  sinAsignarKg: number
}

const TOLERANCIA_KG = 0.05

interface Segmento {
  nombre: string
  kg: number
  colorClase: string
}

export function BarraComposicion({ pesoKg, vendibleKg, hueso, grasa, merma, sinAsignarKg }: BarraComposicionProps) {
  const sinAsignarParaLaBarra = Math.max(0, sinAsignarKg)
  const segmentos: Segmento[] = [
    { nombre: 'Vendible', kg: vendibleKg, colorClase: 'bg-vendible' },
    { nombre: 'Hueso', kg: hueso, colorClase: 'bg-hueso' },
    { nombre: 'Grasa', kg: grasa, colorClase: 'bg-grasa' },
    { nombre: 'Merma', kg: merma, colorClase: 'bg-merma' },
    { nombre: 'Sin asignar', kg: sinAsignarParaLaBarra, colorClase: 'bg-fondo-suave' },
  ]

  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-baseline justify-between">
        <h3 className="text-titulo-seccion font-titulos font-bold text-texto">Composición</h3>
        <MensajeDeControl sinAsignarKg={sinAsignarKg} />
      </div>

      <div className="flex h-6 w-full overflow-hidden rounded-full border border-borde">
        {segmentos.map((segmento) => (
          <div
            key={segmento.nombre}
            className={segmento.colorClase}
            style={{ width: `${pesoKg > 0 ? (segmento.kg / pesoKg) * 100 : 0}%` }}
            title={segmento.nombre}
          />
        ))}
      </div>

      <ul className="flex flex-wrap gap-4 text-cuerpo text-texto-secundario">
        {segmentos.map((segmento) => {
          const porcentaje = pesoKg > 0 ? (segmento.kg / pesoKg) * 100 : 0
          return (
            <li key={segmento.nombre} className="numero">
              {`${segmento.nombre}: ${formatearKg(segmento.kg)} (${new Intl.NumberFormat('es-AR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(porcentaje)} %)`}
            </li>
          )
        })}
      </ul>
    </div>
  )
}

function MensajeDeControl({ sinAsignarKg }: { sinAsignarKg: number }) {
  if (Math.abs(sinAsignarKg) <= TOLERANCIA_KG) {
    return <p className="rounded-xl bg-exito-fondo px-3 py-1 text-cuerpo text-exito-texto">Cuadra con el peso de entrada</p>
  }
  if (sinAsignarKg > 0) {
    return (
      <p className="rounded-xl bg-aviso-fondo px-3 py-1 text-cuerpo text-aviso-texto">
        Faltan asignar {formatearKg(sinAsignarKg)}
      </p>
    )
  }
  return (
    <p className="rounded-xl bg-error-fondo px-3 py-1 text-cuerpo text-error">
      Sobran {formatearKg(Math.abs(sinAsignarKg))}: revisá las pesadas
    </p>
  )
}
