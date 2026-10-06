import { describe, expect, it } from 'vitest'
import { TIPOS_DE_ENTRADA, cortesHabilitados } from './tiposDeEntrada'

describe('tiposDeEntrada', () => {
  it('tiene las 7 opciones en el orden de la tabla de mapeo, cada una con su etiqueta', () => {
    expect(TIPOS_DE_ENTRADA.map((t) => t.valor)).toEqual([
      'MediaRes',
      'Delantero',
      'Pecho',
      'Parrillero',
      'AsadoCompleto',
      'Mocho',
      'Rueda',
    ])
    expect(TIPOS_DE_ENTRADA.find((t) => t.valor === 'MediaRes')?.etiqueta).toBe('Media res')
    expect(TIPOS_DE_ENTRADA.find((t) => t.valor === 'AsadoCompleto')?.etiqueta).toBe('Asado completo')
  })

  it('"MediaRes" no restringe nada (null)', () => {
    expect(cortesHabilitados('MediaRes')).toBeNull()
  })

  it('"Pecho" habilita exactamente Paleta, Roast beef, Cogote y Falda', () => {
    const habilitados = cortesHabilitados('Pecho')
    expect(habilitados).toEqual(new Set(['Paleta', 'Roast beef', 'Cogote', 'Falda']))
  })

  it('"Rueda" es "Mocho" sin Cuadril', () => {
    const mocho = cortesHabilitados('Mocho')
    const rueda = cortesHabilitados('Rueda')
    expect(mocho?.has('Cuadril')).toBe(true)
    expect(rueda?.has('Cuadril')).toBe(false)
    for (const corte of rueda ?? []) {
      expect(mocho?.has(corte)).toBe(true)
    }
  })

  it('"Delantero" (tipo de entrada) incluye Matambre y Vacío', () => {
    const habilitados = cortesHabilitados('Delantero')
    expect(habilitados?.has('Matambre')).toBe(true)
    expect(habilitados?.has('Vacío')).toBe(true)
  })
})
