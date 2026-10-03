import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { TarjetasPerdida } from './TarjetasPerdida'

describe('TarjetasPerdida', () => {
  it('muestra las tres tarjetas con su kg y porcentaje sobre la media res', () => {
    render(
      <TarjetasPerdida pesoKg={100} valores={{ hueso: '11', grasa: '6', merma: '2' }} onCambiar={() => {}} />,
    )

    expect(screen.getByLabelText(/hueso/i)).toHaveValue('11')
    expect(screen.getByText('11,0 %')).toBeInTheDocument()
    expect(screen.getByLabelText(/grasa/i)).toHaveValue('6')
    expect(screen.getByText('6,0 %')).toBeInTheDocument()
    expect(screen.getByLabelText(/merma/i)).toHaveValue('2')
    expect(screen.getByText('2,0 %')).toBeInTheDocument()
  })

  it('editar el campo de grasa llama a onCambiar con el tipo correcto', async () => {
    const onCambiar = vi.fn()
    render(<TarjetasPerdida pesoKg={100} valores={{ hueso: '', grasa: '', merma: '' }} onCambiar={onCambiar} />)

    await userEvent.type(screen.getByLabelText(/grasa/i), '6')

    expect(onCambiar).toHaveBeenCalledWith('grasa', '6')
  })
})
