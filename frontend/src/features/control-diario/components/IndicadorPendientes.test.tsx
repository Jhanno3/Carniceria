import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { IndicadorPendientes } from './IndicadorPendientes'
import { useColaPendiente } from '../api/useColaPendiente'

vi.mock('../api/useColaPendiente')

const useColaPendienteMock = vi.mocked(useColaPendiente)

describe('IndicadorPendientes', () => {
  it('sin pendientes, lo dice explícitamente (FR-303: siempre visible)', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useColaPendienteMock.mockReturnValue({ data: 0 } as any)
    render(<IndicadorPendientes />)
    expect(screen.getByText(/sin pendientes/i)).toBeInTheDocument()
  })

  it('con pendientes, muestra la cantidad', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useColaPendienteMock.mockReturnValue({ data: 3 } as any)
    render(<IndicadorPendientes />)
    expect(screen.getByText(/3.*pendientes de subir/i)).toBeInTheDocument()
  })

  it('con una sola pendiente, usa singular', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useColaPendienteMock.mockReturnValue({ data: 1 } as any)
    render(<IndicadorPendientes />)
    expect(screen.getByText(/1.*venta pendiente de subir/i)).toBeInTheDocument()
  })
})
