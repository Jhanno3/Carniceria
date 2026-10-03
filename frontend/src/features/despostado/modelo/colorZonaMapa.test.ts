import { describe, expect, it } from 'vitest'
import { colorZonaMapa } from './colorZonaMapa'

// Escala de especificacion-carniceria.md, sección 5.1: de rgb(246,228,224) a rgb(110,20,20).
describe('colorZonaMapa', () => {
  it('t=0 (sin kilos) da el extremo claro', () => {
    expect(colorZonaMapa(0)).toBe('rgb(246, 228, 224)')
  })

  it('t=1 (la zona más pesada) da el extremo oscuro', () => {
    expect(colorZonaMapa(1)).toBe('rgb(110, 20, 20)')
  })

  it('t=0.5 da el punto medio exacto de la interpolación', () => {
    expect(colorZonaMapa(0.5)).toBe('rgb(178, 124, 122)')
  })

  it('valores fuera de [0,1] se acotan a los extremos', () => {
    expect(colorZonaMapa(-1)).toBe('rgb(246, 228, 224)')
    expect(colorZonaMapa(2)).toBe('rgb(110, 20, 20)')
  })
})
