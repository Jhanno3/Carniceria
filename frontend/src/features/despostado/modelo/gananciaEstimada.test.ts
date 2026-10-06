import { describe, expect, it } from 'vitest'
import { calcularGananciaEstimada } from './gananciaEstimada'

describe('calcularGananciaEstimada', () => {
  it('ejemplo del dueño: asado a $18.500/kg y espinazo a $5.000/kg, costo $12.319/kg', () => {
    // 7 kg de asado, 2,5 kg de espinazo (kg de ejemplo de "Restablecer ejemplo").
    const ganancia = calcularGananciaEstimada(
      { asado: 7, espinazo: 2.5 },
      12319,
      { asado: 18500, espinazo: 5000 },
    )

    // (18500-12319)*7 + (5000-12319)*2,5 = 43267 + (-18297,5) = 24969,5 -> redondeo a 24970.
    expect(ganancia).toBe(24970)
  })

  it('sin costoKgVendible (sin precio de compra cargado), devuelve null', () => {
    const ganancia = calcularGananciaEstimada({ asado: 7 }, null, { asado: 18500 })
    expect(ganancia).toBeNull()
  })

  it('ningún corte con precioVenta cargado, devuelve null (no es $0)', () => {
    const ganancia = calcularGananciaEstimada({ asado: 7, espinazo: 2.5 }, 12319, {
      asado: null,
      espinazo: null,
    })
    expect(ganancia).toBeNull()
  })

  it('un corte sin precioVenta no aporta ni resta, el resto sigue sumando', () => {
    const conAmbos = calcularGananciaEstimada({ asado: 7, espinazo: 2.5 }, 12319, {
      asado: 18500,
      espinazo: 5000,
    })
    const sinEspinazoConPrecio = calcularGananciaEstimada({ asado: 7, espinazo: 2.5 }, 12319, {
      asado: 18500,
      espinazo: null,
    })

    expect(sinEspinazoConPrecio).toBe(Math.round((18500 - 12319) * 7))
    expect(sinEspinazoConPrecio).not.toBe(conAmbos)
  })

  it('un corte con precioVenta pero kg en cero no aporta', () => {
    const ganancia = calcularGananciaEstimada({ asado: 7, espinazo: 0 }, 12319, {
      asado: 18500,
      espinazo: 5000,
    })

    expect(ganancia).toBe(Math.round((18500 - 12319) * 7))
  })
})
