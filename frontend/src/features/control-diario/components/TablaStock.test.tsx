import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { TablaStock } from './TablaStock'
import type { StockCorteResponse } from '../api/types'

const stockDeEjemplo: StockCorteResponse[] = [
  { corteId: '1', corteNombre: 'Vacío', entradoKg: '10.000', vendidoKg: '8.700', stockKg: '1.300', quedaPoco: true },
  { corteId: '2', corteNombre: 'Asado', entradoKg: '5.000', vendidoKg: '0.000', stockKg: '5.000', quedaPoco: false },
]

describe('TablaStock', () => {
  it('una fila con quedaPoco se marca en naranja y con la etiqueta de texto "Queda poco"', () => {
    render(<TablaStock stock={stockDeEjemplo} />)

    const filaVacio = screen.getByText('Vacío').closest('tr')
    expect(filaVacio).toHaveClass('bg-aviso-fondo')
    expect(filaVacio).toHaveTextContent('Queda poco')

    const filaAsado = screen.getByText('Asado').closest('tr')
    expect(filaAsado).not.toHaveClass('bg-aviso-fondo')
    expect(filaAsado).not.toHaveTextContent('Queda poco')
  })

  it('muestra entrado/vendido/queda con formato es-AR', () => {
    render(<TablaStock stock={stockDeEjemplo} />)
    expect(screen.getByText(/10,0 kg/)).toBeInTheDocument()
    expect(screen.getByText(/8,7 kg/)).toBeInTheDocument()
  })
})
