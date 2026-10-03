import { describe, expect, it } from 'vitest'
import { calcularResumenMes } from './resumenMes'
import type { MediaResResponse } from '../../despostado/api/types'

function entrada(pesoKg: string, vendibleKg: string, costoTotal: string | null): MediaResResponse {
  return {
    id: crypto.randomUUID(),
    fecha: '2026-10-01',
    proveedor: null,
    pesoKg,
    precioKg: costoTotal == null ? null : '5200',
    despostado: [],
    perdidas: { hueso: null, grasa: null, merma: null },
    resumen: {
      vendibleKg,
      perdidaKg: '0',
      sinAsignarKg: '0',
      rendimientoPorc: '0',
      costoTotal,
      costoKgVendible: costoTotal == null ? null : String(Number(costoTotal) / Number(vendibleKg)),
    },
  }
}

describe('calcularResumenMes', () => {
  it('sin entradas, todo en cero o null', () => {
    const resumen = calcularResumenMes([])
    expect(resumen.cantidadEntradas).toBe(0)
    expect(resumen.vendibleKgTotal).toBe(0)
    expect(resumen.rendimientoPromedioPorc).toBeNull()
    expect(resumen.costoKgVendiblePromedio).toBeNull()
  })

  it('promedia rendimiento ponderado por peso, no como promedio simple de porcentajes', () => {
    // 100kg con 81 vendibles (81%) + 50kg con 25 vendibles (50%): promedio simple daría 65,5%,
    // el ponderado correcto es 106/150 = 70,67%.
    const resumen = calcularResumenMes([entrada('100', '81', null), entrada('50', '25', null)])
    expect(resumen.cantidadEntradas).toBe(2)
    expect(resumen.vendibleKgTotal).toBe(106)
    expect(resumen.rendimientoPromedioPorc).toBeCloseTo((106 / 150) * 100, 5)
  })

  it('el costo promedio ignora las entradas sin precio de compra cargado', () => {
    const resumen = calcularResumenMes([
      entrada('100', '81', '520000'), // $6.420/kg vendible
      entrada('50', '25', null), // sin precio, no debe contar
    ])
    expect(resumen.vendibleKgTotal).toBe(106) // sigue sumando los kilos igual
    expect(resumen.costoKgVendiblePromedio).toBeCloseTo(520000 / 81, 5)
  })
})
