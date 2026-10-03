// Dominio puro. Ver especificacion-carniceria.md, sección 5.1 (escala del mapa) y
// research.md, "Color de zona en el mapa SVG".

const CLARO = [246, 228, 224] as const
const OSCURO = [110, 20, 20] as const

function acotar(t: number): number {
  return Math.min(1, Math.max(0, t))
}

/**
 * @param t 0 = la zona no tiene kilos cargados, 1 = es la zona más pesada de la media res.
 */
export function colorZonaMapa(t: number): string {
  const proporcion = acotar(t)
  const [r, g, b] = CLARO.map((canalClaro, indice) =>
    Math.round(canalClaro + (OSCURO[indice] - canalClaro) * proporcion),
  )
  return `rgb(${r}, ${g}, ${b})`
}
