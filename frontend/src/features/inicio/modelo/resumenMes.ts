import type { MediaResResponse } from '../../despostado/api/types'

export interface ResumenMes {
  cantidadEntradas: number
  vendibleKgTotal: number
  /** Promedio ponderado por vendibleKg, no promedio simple de porcentajes. */
  rendimientoPromedioPorc: number | null
  /** Promedio ponderado (costoTotal/vendibleKg); ignora entradas sin precio de compra cargado. */
  costoKgVendiblePromedio: number | null
}

/** Las cadenas numéricas que devuelve el backend usan punto decimal (BigDecimal.toString()),
 * no el formato es-AR de los campos que tipea el usuario — Number() las interpreta bien tal cual. */
function aNumero(texto: string | null): number | null {
  if (texto == null) return null
  const valor = Number(texto)
  return Number.isNaN(valor) ? null : valor
}

export function calcularResumenMes(entradas: MediaResResponse[]): ResumenMes {
  let pesoKgTotal = 0
  let vendibleKgTotal = 0
  let costoTotalConPrecio = 0
  let vendibleKgConPrecio = 0

  for (const entrada of entradas) {
    const pesoKg = aNumero(entrada.pesoKg) ?? 0
    const vendibleKg = aNumero(entrada.resumen.vendibleKg) ?? 0
    pesoKgTotal += pesoKg
    vendibleKgTotal += vendibleKg

    const costoTotal = aNumero(entrada.resumen.costoTotal)
    if (costoTotal != null) {
      costoTotalConPrecio += costoTotal
      vendibleKgConPrecio += vendibleKg
    }
  }

  return {
    cantidadEntradas: entradas.length,
    vendibleKgTotal,
    rendimientoPromedioPorc: pesoKgTotal > 0 ? (vendibleKgTotal / pesoKgTotal) * 100 : null,
    costoKgVendiblePromedio: vendibleKgConPrecio > 0 ? costoTotalConPrecio / vendibleKgConPrecio : null,
  }
}
