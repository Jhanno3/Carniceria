import { describe, expect, it } from 'vitest'
import { validarDigitoVerificador } from './ean13'

// Mismos vectores que backend/.../escaneo/modelo/Ean13Test.java (plan-fase3.md, 3.1):
// el decodificador se duplica acá solo para el modo offline, la autoridad sigue siendo
// el backend cuando hay conexión.
describe('validarDigitoVerificador', () => {
  it('código de la especificación es válido', () => {
    // especificacion-carniceria.md, sección 7: PLU 12 (Vacío), 1,250 kg.
    expect(validarDigitoVerificador('2000012012501')).toBe(true)
  })

  it('mismo código con dígito verificador adulterado es inválido', () => {
    expect(validarDigitoVerificador('2000012012502')).toBe(false)
  })

  it('otro código válido armado a mano', () => {
    // 20 00034 05000: primeros 12 dígitos 200003405000, suma = 2+0*3+0+0*3+0+3*3+4+0*3+5+0*3+0+0*3 = 20 → dígito 0.
    expect(validarDigitoVerificador('2000034050000')).toBe(true)
  })

  it('largo distinto de 13 es inválido sin excepción', () => {
    expect(validarDigitoVerificador('12345')).toBe(false)
    expect(validarDigitoVerificador('')).toBe(false)
  })

  it('con caracteres no numéricos es inválido sin excepción', () => {
    expect(validarDigitoVerificador('20000A2012501')).toBe(false)
  })

  it('null/undefined es inválido sin excepción', () => {
    expect(validarDigitoVerificador(null)).toBe(false)
    expect(validarDigitoVerificador(undefined)).toBe(false)
  })
})
