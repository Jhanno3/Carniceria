// especificacion-carniceria.md, sección 5.4: formato es-AR (coma decimal, punto de
// miles), campos que aceptan coma o punto, kilos con 1 decimal en pantalla.

const FORMATO_KG = new Intl.NumberFormat('es-AR', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})

const FORMATO_PESOS = new Intl.NumberFormat('es-AR', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 0,
})

/** Acepta "1250,5", "1250.5" o el formato es-AR completo "1.250,5". */
export function parsearNumero(texto: string): number {
  const limpio = texto.trim()
  const tieneComaYPunto = limpio.includes(',') && limpio.includes('.')
  const normalizado = tieneComaYPunto
    ? limpio.replace(/\./g, '').replace(',', '.')
    : limpio.replace(',', '.')

  const valor = Number(normalizado)
  if (Number.isNaN(valor)) {
    throw new Error(`"${texto}" no es un número válido.`)
  }
  return valor
}

export function formatearKg(kg: number): string {
  return `${FORMATO_KG.format(kg)} kg`
}

/** Pesos argentinos sin centavos — ver research.md, "Dónde viven las fórmulas de cálculo". */
export function formatearPesos(pesos: number): string {
  return `$ ${FORMATO_PESOS.format(pesos)}`
}
