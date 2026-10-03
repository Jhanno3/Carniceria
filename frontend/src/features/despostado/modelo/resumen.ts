// Dominio puro — ver research.md, "Dónde viven las fórmulas de cálculo": esta es la
// implementación en vivo (antes de guardar), espejo de ResumenDespostado en el backend.
// Mismos casos numéricos obligatorios en los dos lados (ver resumen.test.ts).

const ESCALA_KG = 1000 // 3 decimales, como numeric(8,3) en la base.
const ESCALA_PORCENTAJE = 100 // 2 decimales.

export interface Perdidas {
  hueso: number
  grasa: number
  merma: number
}

export interface ResumenInput {
  pesoKg: number
  precioKg: number | null
  /** corteId → kg cargados para ese corte. */
  kgPorCorte: Record<string, number>
  perdidas: Perdidas
}

export interface Resumen {
  vendibleKg: number
  perdidaKg: number
  sinAsignarKg: number
  rendimientoPorc: number
  /** Pesos argentinos sin centavos (research.md: ningún ejemplo de moneda de la especificación tiene decimales). */
  costoTotal: number | null
  costoKgVendible: number | null
}

/** Suma kilos trabajando en gramos enteros, para no acumular error de punto flotante. */
function sumarKg(...valoresKg: number[]): number {
  const gramos = valoresKg.reduce((total, kg) => total + Math.round(kg * ESCALA_KG), 0)
  return gramos / ESCALA_KG
}

function redondear(valor: number, escala: number): number {
  return Math.round(valor * escala) / escala
}

export function calcularResumen(input: ResumenInput): Resumen {
  if (!(input.pesoKg > 0)) {
    throw new Error('El peso de entrada debe ser mayor a cero.')
  }

  const vendibleKg = sumarKg(...Object.values(input.kgPorCorte))
  const perdidaKg = sumarKg(input.perdidas.hueso, input.perdidas.grasa, input.perdidas.merma)
  const sinAsignarKg = sumarKg(input.pesoKg, -vendibleKg, -perdidaKg)
  const rendimientoPorc = redondear((vendibleKg * 100) / input.pesoKg, ESCALA_PORCENTAJE)

  const costoTotal = input.precioKg == null ? null : Math.round(input.pesoKg * input.precioKg)
  const costoKgVendible =
    costoTotal == null || vendibleKg === 0 ? null : Math.round(costoTotal / vendibleKg)

  return { vendibleKg, perdidaKg, sinAsignarKg, rendimientoPorc, costoTotal, costoKgVendible }
}
