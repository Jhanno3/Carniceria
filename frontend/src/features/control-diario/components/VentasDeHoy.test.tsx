import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { VentasDeHoy } from './VentasDeHoy'
import type { VentaResponse } from '../api/types'

function venta(id: string, hora: string, corteNombre: string): VentaResponse {
  return {
    id,
    fechaHora: `2026-10-10T${hora}:00-03:00`,
    corteId: 'c1',
    corteNombre,
    kg: '1.000',
    codigoLeido: '',
    anulada: false,
  }
}

describe('VentasDeHoy', () => {
  it('sin ventas, muestra un mensaje neutro', () => {
    render(<VentasDeHoy ventas={[]} />)
    expect(screen.getByText(/todavía no hay ventas/i)).toBeInTheDocument()
  })

  it('lista las ventas en el orden recibido (más reciente primero)', () => {
    const ventas = [venta('3', '15:00', 'Asado'), venta('2', '14:00', 'Vacío'), venta('1', '13:00', 'Paleta')]
    render(<VentasDeHoy ventas={ventas} />)

    const nombres = screen.getAllByText(/Asado|Vacío|Paleta/).map((el) => el.textContent)
    expect(nombres).toEqual(['Asado', 'Vacío', 'Paleta'])
  })

  it('muestra como mucho 5 al principio y "Ver todas" las revela', async () => {
    const ventas = Array.from({ length: 7 }, (_, i) => venta(String(i), `1${i}:00`, `Corte ${i}`))
    render(<VentasDeHoy ventas={ventas} />)

    expect(screen.queryByText('Corte 6')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /ver todas/i })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /ver todas/i }))

    expect(screen.getByText('Corte 6')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /ver todas/i })).not.toBeInTheDocument()
  })
})
