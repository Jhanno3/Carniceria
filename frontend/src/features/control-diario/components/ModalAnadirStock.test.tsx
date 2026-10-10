import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ModalAnadirStock } from './ModalAnadirStock'
import { useCortes } from '../../../shared/api/useCortes'
import { useAnadirStock } from '../api/useAnadirStock'
import type { CorteResponse } from '../../../shared/api/types'

vi.mock('../../../shared/api/useCortes')
vi.mock('../api/useAnadirStock')

const useCortesMock = vi.mocked(useCortes)
const useAnadirStockMock = vi.mocked(useAnadirStock)

const CORTES: CorteResponse[] = [
  { id: 'v1', nombre: 'Vacío', plu: 12, cuarto: 'Trasero', tipoProducto: 'Vacuno', zonaMapa: 'vacio', activo: true, precioVenta: null, descuentaStockDeCorteId: null },
  { id: 'a1', nombre: 'Chorizo', plu: 40, cuarto: null, tipoProducto: 'AchurasEmbutidos', zonaMapa: null, activo: true, precioVenta: null, descuentaStockDeCorteId: null },
  { id: 'c1', nombre: 'Bondiola', plu: 41, cuarto: null, tipoProducto: 'Cerdo', zonaMapa: null, activo: true, precioVenta: null, descuentaStockDeCorteId: null },
  { id: 'c2', nombre: 'Inactivo', plu: 42, cuarto: null, tipoProducto: 'Cerdo', zonaMapa: null, activo: false, precioVenta: null, descuentaStockDeCorteId: null },
  { id: 'k1', nombre: 'Rabo', plu: 31, cuarto: null, tipoProducto: 'Carne', zonaMapa: null, activo: true, precioVenta: null, descuentaStockDeCorteId: null },
]

function mockUseCortes(data: CorteResponse[] | undefined) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  useCortesMock.mockReturnValue({ data } as any)
}

function mockUseAnadirStock(mutateAsync = vi.fn()) {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  useAnadirStockMock.mockReturnValue({ mutateAsync, isPending: false } as any)
  return mutateAsync
}

describe('ModalAnadirStock', () => {
  it('el selector de corte arranca deshabilitado hasta elegir un tipo', () => {
    mockUseCortes(CORTES)
    mockUseAnadirStock()
    render(<ModalAnadirStock onCerrar={vi.fn()} />)

    expect(screen.getByLabelText(/^corte$/i)).toBeDisabled()
  })

  it('el tipo "Cerdo" solo lista cortes de Cerdo activos (ni Achuras ni Vacuno ni inactivos)', async () => {
    mockUseCortes(CORTES)
    mockUseAnadirStock()
    const usuario = userEvent.setup()
    render(<ModalAnadirStock onCerrar={vi.fn()} />)

    await usuario.selectOptions(screen.getByLabelText(/tipo de corte/i), 'Cerdo')

    expect(screen.getByLabelText(/^corte$/i)).not.toBeDisabled()
    expect(screen.getByText('Bondiola')).toBeInTheDocument()
    expect(screen.queryByText('Chorizo')).not.toBeInTheDocument()
    expect(screen.queryByText('Vacío')).not.toBeInTheDocument()
    expect(screen.queryByText('Inactivo')).not.toBeInTheDocument()
  })

  it('cambiar de tipo limpia el corte elegido antes', async () => {
    mockUseCortes(CORTES)
    mockUseAnadirStock()
    const usuario = userEvent.setup()
    render(<ModalAnadirStock onCerrar={vi.fn()} />)

    await usuario.selectOptions(screen.getByLabelText(/tipo de corte/i), 'Cerdo')
    await usuario.selectOptions(screen.getByLabelText(/^corte$/i), 'Bondiola')
    await usuario.selectOptions(screen.getByLabelText(/tipo de corte/i), 'Carne')

    expect(screen.getByLabelText(/^corte$/i)).toHaveValue('')
    expect(screen.getByText('Rabo')).toBeInTheDocument()
    expect(screen.queryByText('Bondiola')).not.toBeInTheDocument()
  })

  it('sin ningún corte no-Vacuno disponible, muestra el aviso y Guardar queda deshabilitado', () => {
    mockUseCortes([CORTES[0]]) // solo el Vacuno
    mockUseAnadirStock()
    render(<ModalAnadirStock onCerrar={vi.fn()} />)

    expect(screen.getByText(/no ten[eé]s cortes de achuras/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /guardar/i })).toBeDisabled()
  })

  it('con kg <= 0, no deja guardar y muestra un error', async () => {
    mockUseCortes(CORTES)
    const mutateAsync = mockUseAnadirStock()
    const usuario = userEvent.setup()
    render(<ModalAnadirStock onCerrar={vi.fn()} />)

    await usuario.selectOptions(screen.getByLabelText(/tipo de corte/i), 'AchurasEmbutidos')
    await usuario.selectOptions(screen.getByLabelText(/^corte$/i), 'Chorizo')
    await usuario.type(screen.getByLabelText(/kilos/i), '0')
    await usuario.click(screen.getByRole('button', { name: /guardar/i }))

    expect(mutateAsync).not.toHaveBeenCalled()
    expect(screen.getByText(/mayores a 0/i)).toBeInTheDocument()
  })

  it('con tipo, corte y kg válidos, llama a la mutación con el body esperado y muestra la confirmación', async () => {
    mockUseCortes(CORTES)
    const mutateAsync = mockUseAnadirStock()
    const usuario = userEvent.setup()
    render(<ModalAnadirStock onCerrar={vi.fn()} />)

    await usuario.selectOptions(screen.getByLabelText(/tipo de corte/i), 'Cerdo')
    await usuario.selectOptions(screen.getByLabelText(/^corte$/i), 'Bondiola')
    await usuario.type(screen.getByLabelText(/kilos/i), '5')
    await usuario.type(screen.getByLabelText(/precio de compra/i), '3500')
    await usuario.click(screen.getByRole('button', { name: /guardar/i }))

    expect(mutateAsync).toHaveBeenCalledWith({
      corteId: 'c1',
      kg: '5',
      precioKg: '3500',
      tipoEntrada: 'Cerdo',
    })
    expect(await screen.findByText(/stock añadido/i)).toBeInTheDocument()
  })

  it('después de la confirmación, cierra el modal solo', async () => {
    mockUseCortes(CORTES)
    mockUseAnadirStock()
    const onCerrar = vi.fn()
    const usuario = userEvent.setup()
    render(<ModalAnadirStock onCerrar={onCerrar} />)

    await usuario.selectOptions(screen.getByLabelText(/tipo de corte/i), 'Cerdo')
    await usuario.selectOptions(screen.getByLabelText(/^corte$/i), 'Bondiola')
    await usuario.type(screen.getByLabelText(/kilos/i), '5')
    await usuario.click(screen.getByRole('button', { name: /guardar/i }))

    expect(await screen.findByText(/stock añadido/i)).toBeInTheDocument()
    expect(onCerrar).not.toHaveBeenCalled()

    await waitFor(() => expect(onCerrar).toHaveBeenCalled(), { timeout: 2000 })
  })

  it('con tipo "Carne" y el corte Rabo, manda tipoEntrada "Carne"', async () => {
    mockUseCortes(CORTES)
    const mutateAsync = mockUseAnadirStock()
    const usuario = userEvent.setup()
    render(<ModalAnadirStock onCerrar={vi.fn()} />)

    await usuario.selectOptions(screen.getByLabelText(/tipo de corte/i), 'Carne')
    await usuario.selectOptions(screen.getByLabelText(/^corte$/i), 'Rabo')
    await usuario.type(screen.getByLabelText(/kilos/i), '2')
    await usuario.click(screen.getByRole('button', { name: /guardar/i }))

    expect(mutateAsync).toHaveBeenCalledWith({
      corteId: 'k1',
      kg: '2',
      precioKg: null,
      tipoEntrada: 'Carne',
    })
  })
})
