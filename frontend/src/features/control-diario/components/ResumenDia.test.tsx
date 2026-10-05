import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ResumenDia } from './ResumenDia'
import type { ResumenDiaResponse } from '../api/types'

const resumenDeEjemplo: ResumenDiaResponse = {
  kgVendidosHoy: '42.800',
  etiquetasEscaneadasHoy: 37,
  stockVendibleTotal: '128.400',
  entradasHoy: 2,
}

describe('ResumenDia', () => {
  it('muestra las 4 cifras con formato es-AR', () => {
    render(<ResumenDia resumen={resumenDeEjemplo} />)

    expect(screen.getByText(/42,8 kg/)).toBeInTheDocument()
    expect(screen.getByText('37')).toBeInTheDocument()
    expect(screen.getByText(/128,4 kg/)).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })
})
