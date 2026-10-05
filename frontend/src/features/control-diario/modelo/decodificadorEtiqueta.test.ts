import { describe, expect, it } from 'vitest'
import { decodificar } from './decodificadorEtiqueta'
import type { ConfigEtiqueta } from './configEtiqueta'

// Mismos vectores que backend/.../escaneo/modelo/DecodificadorEtiquetaTest.java
// (plan-fase3.md, 3.1). No conoce `cortes`, igual que su contraparte Java: devuelve el
// PLU crudo, no un corteId — ese cruce lo hace quien use este decodificador (Bloque 7).
const CONFIG_DE_EJEMPLO: ConfigEtiqueta = {
  prefijoDesde: 20,
  prefijoHasta: 29,
  inicioPlu: 2,
  largoPlu: 5,
  inicioValor: 7,
  largoValor: 5,
  tipoValor: 'peso',
  decimales: 3,
}

describe('decodificar', () => {
  it('código de la especificación decodifica PLU y kg', () => {
    const resultado = decodificar('2000012012501', CONFIG_DE_EJEMPLO)

    expect(resultado.tipo).toBe('Exito')
    if (resultado.tipo === 'Exito') {
      expect(resultado.plu).toBe(12)
      expect(resultado.kg).toBeCloseTo(1.25, 3)
    }
  })

  it('dígito verificador inválido devuelve error', () => {
    const resultado = decodificar('2000012012509', CONFIG_DE_EJEMPLO)
    expect(resultado.tipo).toBe('DigitoVerificadorInvalido')
  })

  it('largo distinto de 13 es dígito verificador inválido sin excepción', () => {
    const resultado = decodificar('12345', CONFIG_DE_EJEMPLO)
    expect(resultado.tipo).toBe('DigitoVerificadorInvalido')
  })

  it('prefijo fuera de rango devuelve error', () => {
    // "190003405000" + dígito verificador 4 — prefijo 19, fuera de [20,29].
    const resultado = decodificar('1900034050004', CONFIG_DE_EJEMPLO)
    expect(resultado.tipo).toBe('PrefijoInvalido')
  })

  it('valor en cero devuelve peso cero', () => {
    // "200001200000" + dígito verificador 3 — PLU 12, valor 00000.
    const resultado = decodificar('2000012000003', CONFIG_DE_EJEMPLO)
    expect(resultado.tipo).toBe('PesoCero')
  })
})
