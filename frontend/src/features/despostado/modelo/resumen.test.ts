import { describe, expect, it } from 'vitest'
import { calcularResumen } from './resumen'

const UN_CORTE = 'corte-1'

describe('calcularResumen', () => {
  it('ejemplo obligatorio de la especificación: 100 kg a $5.200, 81 kg vendibles → $6.420 por kg vendible', () => {
    const resumen = calcularResumen({
      pesoKg: 100,
      precioKg: 5200,
      kgPorCorte: { [UN_CORTE]: 81 },
      perdidas: { hueso: 11, grasa: 6, merma: 2 },
    })

    expect(resumen.vendibleKg).toBeCloseTo(81, 3)
    expect(resumen.perdidaKg).toBeCloseTo(19, 3)
    expect(resumen.sinAsignarKg).toBeCloseTo(0, 3)
    expect(resumen.rendimientoPorc).toBeCloseTo(81, 2)
    // Pesos argentinos sin centavos — ver research.md (mismo hallazgo que en el backend).
    expect(resumen.costoTotal).toBe(520000)
    expect(resumen.costoKgVendible).toBe(6420)
  })

  it('sin precio de compra, costoTotal y costoKgVendible son null', () => {
    const resumen = calcularResumen({
      pesoKg: 100,
      precioKg: null,
      kgPorCorte: { [UN_CORTE]: 81 },
      perdidas: { hueso: 11, grasa: 6, merma: 2 },
    })

    expect(resumen.costoTotal).toBeNull()
    expect(resumen.costoKgVendible).toBeNull()
  })

  it('faltan kilos por asignar: sinAsignarKg es positivo', () => {
    const resumen = calcularResumen({
      pesoKg: 100,
      precioKg: 5200,
      kgPorCorte: { [UN_CORTE]: 79 },
      perdidas: { hueso: 11, grasa: 6, merma: 2 },
    })

    expect(resumen.sinAsignarKg).toBeCloseTo(2, 3)
  })

  it('sobran kilos: sinAsignarKg es negativo', () => {
    const resumen = calcularResumen({
      pesoKg: 100,
      precioKg: 5200,
      kgPorCorte: { [UN_CORTE]: 83 },
      perdidas: { hueso: 11, grasa: 6, merma: 2 },
    })

    expect(resumen.sinAsignarKg).toBeCloseTo(-2, 3)
  })

  it('sin kilos vendibles, costoKgVendible es null', () => {
    const resumen = calcularResumen({
      pesoKg: 100,
      precioKg: 5200,
      kgPorCorte: {},
      perdidas: { hueso: 0, grasa: 0, merma: 0 },
    })

    expect(resumen.vendibleKg).toBe(0)
    expect(resumen.costoKgVendible).toBeNull()
  })

  it('muchos cortes con decimales no acumulan error de punto flotante', () => {
    const resumen = calcularResumen({
      pesoKg: 100,
      precioKg: null,
      // Los 21 cortes de ejemplo de especificacion-carniceria.md, sección 2.2 (suman 81 kg exactos).
      kgPorCorte: {
        a: 3.0, b: 1.8, c: 4.5, d: 6, e: 2.5, f: 1.2, g: 3, h: 1.4,
        i: 3.3, j: 1.3, k: 3.5, l: 11, m: 1.5, n: 4, o: 4, p: 7.5,
        q: 6.5, r: 1.5, s: 3.5, t: 3.5, u: 6.5,
      },
      perdidas: { hueso: 11, grasa: 6, merma: 2 },
    })

    expect(resumen.vendibleKg).toBeCloseTo(81, 3);
    expect(resumen.sinAsignarKg).toBeCloseTo(0, 3)
  })

  it('peso de entrada inválido: rechaza el cálculo', () => {
    expect(() =>
      calcularResumen({
        pesoKg: 0,
        precioKg: 5200,
        kgPorCorte: {},
        perdidas: { hueso: 0, grasa: 0, merma: 0 },
      }),
    ).toThrow()
  })
})
