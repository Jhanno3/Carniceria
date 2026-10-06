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

// Coordenadas de referencia (resources/mapa-cortes-completo.html, viewBox "0 20 620 325",
// cabeza a la izquierda) — no reinventar los polígonos. Esa versión tiene 25 zonas y, tras
// V22__reactivar_cortes_redundantes.sql, las 25 tienen corte propio en nuestro catálogo:
//   - "bifeAngosto" usa la forma de su "bifeChorizo" (mismo corte, otro nombre regional).
//   - "nalga" junta sus polígonos "nalga" + "cuadrada" (Nalga, Cuadrada y Tapa de nalga
//     comparten zona en nuestro catálogo).
//   - "bola" junta sus polígonos "bolaDeLomo" + "peceto" (Bola de lomo y Peceto
//     comparten zona).
//   - espinazo/roastBeef/chingolo/tortuga/colitaCuadril/entraña/asadoAmericano (V21):
//     cortes nuevos, cada uno con su propia zona igual que el archivo de referencia.
//   - aguja/marucha/pecho/cogote (V22): sacados en V8__eliminar_cortes_redundantes.sql,
//     reactivados a pedido del dueño — cada uno con su propia zona esta vez (antes Marucha
//     compartía zona con Paleta).
const SILUETA =
  '40,90 110,58 210,50 480,44 555,50 578,110 560,150 552,215 548,215 540,312 505,312 ' +
  '492,215 470,210 440,234 255,247 165,252 160,312 128,312 120,252 85,242 68,195 44,160'

interface Etiqueta {
  x: number
  y: number
  size: number
  rot?: number
}

interface Zona {
  id: string
  nombre: string
  poligonos: string[]
  etiquetas: Etiqueta[]
}

const ZONAS: Zona[] = [
  { id: 'bifeAncho', nombre: 'Bife ancho', poligonos: ['250,53 305,51 305,105 250,105'], etiquetas: [{ x: 277, y: 84, size: 11 }] },
  { id: 'bifeAngosto', nombre: 'Bife angosto', poligonos: ['305,51 400,48 400,104 340,104 305,105'], etiquetas: [{ x: 352, y: 82, size: 10 }] },
  { id: 'cuadril', nombre: 'Cuadril', poligonos: ['400,48 480,45 480,101 400,103'], etiquetas: [{ x: 440, y: 80, size: 11 }] },
  { id: 'lomo', nombre: 'Lomo', poligonos: ['340,104 480,101 480,122 340,122'], etiquetas: [{ x: 410, y: 116, size: 10 }] },
  {
    id: 'nalga',
    nombre: 'Nalga',
    poligonos: ['480,45 555,51 568,90 480,95', '480,95 568,90 575,110 563,140 480,140'],
    etiquetas: [{ x: 525, y: 98, size: 11 }],
  },
  {
    id: 'bola',
    nombre: 'Bola de lomo',
    poligonos: ['480,140 521,140 521,215 490,215', '521,140 563,140 560,150 552,215 521,215'],
    etiquetas: [{ x: 518, y: 178, size: 10 }],
  },
  { id: 'paleta', nombre: 'Paleta', poligonos: ['117,158 196,157 198,198 160,198 120,180'], etiquetas: [{ x: 160, y: 182, size: 11 }] },
  { id: 'asado', nombre: 'Asado', poligonos: ['195,129 322,127 325,192 198,198'], etiquetas: [{ x: 260, y: 166, size: 12 }] },
  { id: 'vacio', nombre: 'Vacío', poligonos: ['340,122 440,122 445,150 480,150 470,198 343,190'], etiquetas: [{ x: 400, y: 165, size: 12 }] },
  { id: 'falda', nombre: 'Falda', poligonos: ['160,198 198,198 262,195 255,245 155,250'], etiquetas: [{ x: 210, y: 226, size: 11 }] },
  { id: 'matambre', nombre: 'Matambre', poligonos: ['262,195 325,192 343,190 470,198 440,232 255,245'], etiquetas: [{ x: 352, y: 220, size: 11 }] },
  {
    id: 'osobuco',
    nombre: 'Osobuco',
    poligonos: ['122,252 163,252 158,310 130,310', '497,250 549,250 540,310 506,310'],
    etiquetas: [
      { x: 144, y: 330, size: 11 },
      { x: 523, y: 330, size: 11 },
    ],
  },
  { id: 'roastBeef', nombre: 'Roast beef', poligonos: ['195,56 250,53 250,105 195,105'], etiquetas: [{ x: 222, y: 84, size: 10 }] },
  { id: 'asadoAmericano', nombre: 'Asado americano', poligonos: ['195,105 305,105 322,104 322,127 195,129'], etiquetas: [{ x: 258, y: 120, size: 9 }] },
  { id: 'chingolo', nombre: 'Chingolo', poligonos: ['115,132 152,131 152,158 117,158'], etiquetas: [{ x: 134, y: 148, size: 8 }] },
  { id: 'entrana', nombre: 'Entraña', poligonos: ['322,104 340,104 343,190 325,192'], etiquetas: [{ x: 333, y: 148, size: 9, rot: -90 }] },
  { id: 'colitaCuadril', nombre: 'Colita de cuadril', poligonos: ['440,122 480,122 480,150 445,150'], etiquetas: [{ x: 461, y: 139, size: 8 }] },
  { id: 'tortuga', nombre: 'Tortuga', poligonos: ['490,215 552,215 549,250 497,250'], etiquetas: [{ x: 521, y: 236, size: 10 }] },
  { id: 'espinazo', nombre: 'Espinazo', poligonos: ['110,58 210,50 480,43 480,52 210,59 110,67'], etiquetas: [{ x: 300, y: 36, size: 11 }] },
  { id: 'cogote', nombre: 'Cogote', poligonos: ['40,90 110,60 120,180 70,195 45,160'], etiquetas: [{ x: 80, y: 140, size: 11 }] },
  { id: 'pecho', nombre: 'Pecho', poligonos: ['70,195 120,180 160,198 155,250 85,240'], etiquetas: [{ x: 115, y: 220, size: 11 }] },
  { id: 'aguja', nombre: 'Aguja', poligonos: ['110,62 195,56 195,128 115,132'], etiquetas: [{ x: 153, y: 98, size: 10 }] },
  { id: 'marucha', nombre: 'Marucha', poligonos: ['152,131 195,128 196,157 152,158'], etiquetas: [{ x: 174, y: 147, size: 8 }] },
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

  return (
    <div className="flex flex-col gap-2">
      <div className="rounded-xl border border-borde bg-fondo-suave-2 p-2">
        <svg
          viewBox="0 20 620 325"
          className="h-auto w-full"
          role="img"
          aria-label="Mapa de cortes de la media res"
        >
          {/* Silueta de fondo: tapa cualquier hueco entre zonas. */}
          <polygon points={SILUETA} fill="#e7e5e4" />

          {ZONAS.map((zona) => {
            const kg = kgPorZona.get(zona.id) ?? 0
            const t = kgMaximo > 0 ? kg / kgMaximo : 0
            const color = colorZonaMapa(t)
            const resaltada = zona.id === zonaResaltada
            return zona.poligonos.map((puntos, indice) => (
              <polygon
                key={`${zona.id}-${indice}`}
                data-testid={indice === 0 ? `zona-${zona.id}` : undefined}
                points={puntos}
                fill={color}
                stroke={resaltada ? '#000000' : '#ffffff'}
                strokeWidth={resaltada ? 4 : 1.5}
                strokeLinejoin="round"
              />
            ))
          })}

          {ZONAS.map((zona) => {
            const kg = kgPorZona.get(zona.id) ?? 0
            const t = kgMaximo > 0 ? kg / kgMaximo : 0
            const color = colorDeTexto(colorZonaMapa(t))
            return zona.etiquetas.map((etiqueta, indice) => (
              <text
                key={`${zona.id}-etiqueta-${indice}`}
                x={etiqueta.x}
                y={etiqueta.y}
                fontSize={etiqueta.size}
                textAnchor="middle"
                dominantBaseline="middle"
                fill={color}
                transform={etiqueta.rot ? `rotate(${etiqueta.rot} ${etiqueta.x} ${etiqueta.y})` : undefined}
                className="pointer-events-none select-none font-semibold"
              >
                {zona.nombre}
              </text>
            ))
          })}
        </svg>
      </div>

      <p className="text-etiqueta text-texto-secundario">
        Más oscuro = más kilos. Tocá un corte en la tabla para ubicarlo.
      </p>

      {detalle ? (
        <p className="rounded-xl bg-rosado px-3 py-2 text-cuerpo text-texto">
          {detalle.nombre} · {detalle.kg.toFixed(1).replace('.', ',')} kg ·{' '}
          {detalle.porcentajeDeLaMediaRes.toFixed(1).replace('.', ',')} % de la media res
        </p>
      ) : (
        <p className="rounded-xl bg-fondo-suave px-3 py-2 text-cuerpo text-texto-secundario">
          Ningún corte seleccionado
        </p>
      )}

      <p className="text-etiqueta text-texto-secundario">
        Esquema orientativo. Carne picada y recortes no tienen zona en el mapa.
      </p>
    </div>
  )
}
