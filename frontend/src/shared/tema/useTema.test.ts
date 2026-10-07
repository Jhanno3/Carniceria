import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { CLAVE_TEMA, useTema } from './useTema'

function mockearPreferenciaDelSistema(prefiereOscuro: boolean) {
  window.matchMedia = vi.fn().mockReturnValue({ matches: prefiereOscuro }) as unknown as typeof window.matchMedia
}

describe('useTema', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('sin preferencia guardada, arranca según prefers-color-scheme del sistema', () => {
    mockearPreferenciaDelSistema(true)
    const { result } = renderHook(() => useTema())
    expect(result.current.tema).toBe('oscuro')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
  })

  it('con preferencia guardada, la respeta por encima del sistema', () => {
    mockearPreferenciaDelSistema(true)
    localStorage.setItem(CLAVE_TEMA, 'claro')
    const { result } = renderHook(() => useTema())
    expect(result.current.tema).toBe('claro')
    expect(document.documentElement.classList.contains('dark')).toBe(false)
  })

  it('alternarTema cambia el tema, la clase del <html> y lo persiste', () => {
    mockearPreferenciaDelSistema(false)
    const { result } = renderHook(() => useTema())

    act(() => result.current.alternarTema())

    expect(result.current.tema).toBe('oscuro')
    expect(document.documentElement.classList.contains('dark')).toBe(true)
    expect(localStorage.getItem(CLAVE_TEMA)).toBe('oscuro')
  })
})
