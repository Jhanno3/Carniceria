import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ResumenDia } from './ResumenDia'
import type { ResumenDiaResponse } from '../api/types'

const resumenDeEjemplo: ResumenDiaResponse = {
  kgVendidosHoy: '42.800',
  etiquetasEscaneadasHoy: 37,
  stockVendibleTotal: '128.400',
  entradasHoy: 2,
  dineroRecaudadoHoy: '14125',
  ventasSinPrecioHoy: 0,
}

describe('ResumenDia', () => {
  it('muestra las 4 cifras con formato es-AR', () => {
    render(<ResumenDia resumen={resumenDeEjemplo} />)

    expect(screen.getByText(/42,8 kg/)).toBeInTheDocument()
    expect(screen.getByText('37')).toBeInTheDocument()
    expect(screen.getByText(/128,4 kg/)).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('muestra el dinero recaudado hoy', () => {
    render(<ResumenDia resumen={resumenDeEjemplo} />)

    expect(screen.getByText('$ 14.125')).toBeInTheDocument()
  })

  it('sin ventas sin precio, no muestra ninguna aclaración', () => {
    render(<ResumenDia resumen={resumenDeEjemplo} />)

    expect(screen.queryByText(/sin precio registrado/i)).not.toBeInTheDocument()
  })

  it('con ventas sin precio, muestra cuántas', () => {
    render(<ResumenDia resumen={{ ...resumenDeEjemplo, ventasSinPrecioHoy: 3 }} />)

    expect(screen.getByText(/3 ventas de hoy sin precio registrado/i)).toBeInTheDocument()
  })
})
