import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { SelectorModoCarga } from './SelectorModoCarga'

describe('SelectorModoCarga', () => {
  it('manual siempre está habilitado', async () => {
    const onElegir = vi.fn()
    render(<SelectorModoCarga disponibleAutomatico={false} onElegir={onElegir} />)

    const botonManual = screen.getByRole('button', { name: /manual/i })
    expect(botonManual).toBeEnabled()
    await userEvent.click(botonManual)
    expect(onElegir).toHaveBeenCalledWith('manual')
  })

  it('automático se deshabilita cuando no hay historial', () => {
    render(<SelectorModoCarga disponibleAutomatico={false} onElegir={() => {}} />)

    expect(screen.getByRole('button', { name: /automátic/i })).toBeDisabled()
  })

  it('automático se habilita cuando sí hay historial', async () => {
    const onElegir = vi.fn()
    render(<SelectorModoCarga disponibleAutomatico={true} onElegir={onElegir} />)

    const botonAutomatico = screen.getByRole('button', { name: /automátic/i })
    expect(botonAutomatico).toBeEnabled()
    await userEvent.click(botonAutomatico)
    expect(onElegir).toHaveBeenCalledWith('automatico')
  })
})
