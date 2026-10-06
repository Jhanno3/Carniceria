// Dominio puro. La "Ganancia estimada" (pedido del dueño, 2026-10) no es
// "precioVenta promedio ponderado - costo por kg" (eso ya existe en Reportes, Fase 5) sino
// la ganancia TOTAL en pesos de esta entrada puntual: para cada corte con kg cargado y
// precioVenta cargado en "Editar cortes", resta el mismo costoKgVendible uniforme de la
// media res (no hay forma de saber el costo "real" de un corte individual dentro de una
// compra conjunta) y multiplica por los kg de ese corte — los cortes caros (asado) suelen
// compensar a los baratos (espinazo), que entran igual con margen negativo.

/** `null` si no hay `costoKgVendible` o si ningún corte con kg cargado tiene `precioVenta` (no es lo mismo que $0). */
export function calcularGananciaEstimada(
  kgPorCorte: Record<string, number>,
  costoKgVendible: number | null,
  precioVentaPorCorte: Record<string, number | null | undefined>,
): number | null {
  if (costoKgVendible == null) {
    return null
  }

  let ganancia = 0
  let huboAlgunCorteConPrecio = false

  for (const [corteId, kg] of Object.entries(kgPorCorte)) {
    const precioVenta = precioVentaPorCorte[corteId]
    if (precioVenta == null || !(kg > 0)) {
      continue
    }
    ganancia += (precioVenta - costoKgVendible) * kg
    huboAlgunCorteConPrecio = true
  }

  return huboAlgunCorteConPrecio ? Math.round(ganancia) : null
}
