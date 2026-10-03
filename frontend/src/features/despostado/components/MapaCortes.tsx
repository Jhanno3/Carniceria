import { colorZonaMapa } from '../modelo/colorZonaMapa'

export interface ZonaMapa {
  id: string
  kg: number
}

interface MapaCortesProps {
  zonas: ZonaMapa[]
  zonaResaltada: string | null
  detalle: { nombre: string; kg: number; porcentajeDeLaMediaRes: number } | null
}

// Orden esquemático, cabeza a la izquierda (especificacion-carniceria.md, sección 4.1):
// delantero primero, osobuco en el medio (es de ambos cuartos), trasero al final.
const ORDEN_ZONAS = [
  'cogote', 'pecho', 'aguja', 'bifeAncho', 'falda', 'paleta', 'asado',
  'osobuco',
  'lomo', 'bifeAngosto', 'nalga', 'bola', 'vacio', 'matambre', 'cuadril',
]

function colorDeTexto(colorFondo: string): string {
  // La misma escala que colorZonaMapa: a partir de la mitad del camino el fondo ya es
  // oscuro. No vale la pena un cálculo de luminancia completo para esto.
  const coincide = colorFondo.match(/\d+/g)
  if (!coincide) return '#1c1917'
  const [r, g, b] = coincide.map(Number)
  const luminancia = (r * 299 + g * 587 + b * 114) / 1000
  return luminancia < 140 ? '#ffffff' : '#1c1917'
}

export function MapaCortes({ zonas, zonaResaltada, detalle }: MapaCortesProps) {
  const kgPorZona = new Map(zonas.map((z) => [z.id, z.kg]))
  const kgMaximo = Math.max(0, ...zonas.map((z) => z.kg))
  const ancho = 60
  const alto = 40

  return (
    <div className="flex flex-col gap-2">
      <div className="overflow-x-auto rounded-xl border border-borde">
      <svg
        viewBox={`0 0 ${ancho * ORDEN_ZONAS.length} ${alto}`}
        className="h-auto w-full"
        style={{ minWidth: `${ancho * ORDEN_ZONAS.length * 4}px` }}
        role="img"
        aria-label="Mapa de cortes de la media res"
      >
        {ORDEN_ZONAS.map((zonaId, indice) => {
          const kg = kgPorZona.get(zonaId) ?? 0
          const t = kgMaximo > 0 ? kg / kgMaximo : 0
          const color = colorZonaMapa(t)
          const resaltada = zonaId === zonaResaltada
          return (
            <g key={zonaId}>
              <rect
                data-testid={`zona-${zonaId}`}
                x={indice * ancho}
                y={0}
                width={ancho}
                height={alto}
                fill={color}
                stroke={resaltada ? '#000000' : '#e7e5e4'}
                strokeWidth={resaltada ? 3 : 1}
              />
              <text
                x={indice * ancho + ancho / 2}
                y={alto / 2}
                fontSize={5}
                textAnchor="middle"
                dominantBaseline="middle"
                fill={colorDeTexto(color)}
              >
                {zonaId}
              </text>
            </g>
          )
        })}
      </svg>
      </div>

      <p className="text-etiqueta text-texto-secundario">
        Esquema orientativo. Carne picada y recortes no tienen zona en el mapa.
      </p>

      {detalle && (
        <p className="text-cuerpo text-texto">
          {detalle.nombre} · {detalle.kg.toFixed(1).replace('.', ',')} kg ·{' '}
          {detalle.porcentajeDeLaMediaRes.toFixed(1).replace('.', ',')} % de la media res
        </p>
      )}
    </div>
  )
}
