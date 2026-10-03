import { describe, expect, it } from 'vitest'
import { formatearKg, formatearPesos, parsearNumero } from './formatoEsAr'

describe('parsearNumero', () => {
  it('acepta coma como separador decimal', () => {
    expect(parsearNumero('1250,5')).toBe(1250.5)
  })

  it('acepta punto como separador decimal', () => {
    expect(parsearNumero('1250.5')).toBe(1250.5)
  })

  it('acepta el formato es-AR completo (punto de miles, coma decimal)', () => {
    expect(parsearNumero('1.250,5')).toBe(1250.5)
  })

  it('un número sin separador decimal se parsea entero', () => {
    expect(parsearNumero('100')).toBe(100)
  })

  it('un texto que no es un número lanza un error', () => {
    expect(() => parsearNumero('abc')).toThrow()
  })
})

describe('formatearKg', () => {
  it('muestra 1 decimal y punto de miles (sección 5.4)', () => {
    expect(formatearKg(1250.5)).toBe('1.250,5 kg')
  })

  it('redondea a 1 decimal', () => {
    expect(formatearKg(81)).toBe('81,0 kg')
  })
})

describe('formatearPesos', () => {
  it('sin centavos, con punto de miles y el signo "$ " (sección 5.4 y 6)', () => {
    expect(formatearPesos(6420)).toBe('$ 6.420')
    expect(formatearPesos(520000)).toBe('$ 520.000')
  })
})
