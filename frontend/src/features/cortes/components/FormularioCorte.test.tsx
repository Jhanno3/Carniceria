import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { FormularioCorte } from './FormularioCorte'
import type { Corte } from '../api/types'

const corteExistente: Corte = {
  id: 'c1',
  nombre: 'Vacío',
  plu: 12,
  cuarto: 'Trasero',
  tipoProducto: 'Vacuno',
  zonaMapa: 'vacio',
  activo: true,
  precioVenta: '6500.00',
}

describe('FormularioCorte', () => {
  it('alta: no deja guardar sin nombre ni PLU', async () => {
    const onGuardar = vi.fn()
    const usuario = userEvent.setup()
    render(<FormularioCorte corteExistente={null} onGuardar={onGuardar} onCancelar={vi.fn()} guardando={false} />)

    await usuario.click(screen.getByRole('button', { name: /guardar/i }))

    expect(onGuardar).not.toHaveBeenCalled()
    expect(screen.getByText(/no puede quedar vacío/i)).toBeInTheDocument()
  })

  it('alta: con nombre y PLU completos, llama a onGuardar con los valores tal cual los tipeó', async () => {
    const onGuardar = vi.fn()
    const usuario = userEvent.setup()
    render(<FormularioCorte corteExistente={null} onGuardar={onGuardar} onCancelar={vi.fn()} guardando={false} />)

    await usuario.type(screen.getByLabelText(/^nombre$/i), 'Corte nuevo')
    await usuario.type(screen.getByLabelText(/^plu$/i), '9001')
    await usuario.type(screen.getByLabelText(/precio de venta/i), '9.500,50')
    await usuario.click(screen.getByRole('button', { name: /guardar/i }))

    expect(onGuardar).toHaveBeenCalledWith({
      nombre: 'Corte nuevo',
      plu: '9001',
      cuarto: 'Ambos',
      tipoProducto: 'Vacuno',
      zonaMapa: '',
      activo: true,
      precioVenta: '9.500,50',
    })
  })

  it('edición: precarga los valores del corte existente', () => {
    render(<FormularioCorte corteExistente={corteExistente} onGuardar={vi.fn()} onCancelar={vi.fn()} guardando={false} />)

    expect(screen.getByLabelText(/^nombre$/i)).toHaveValue('Vacío')
    expect(screen.getByLabelText(/^plu$/i)).toHaveValue('12')
    expect(screen.getByLabelText(/precio de venta/i)).toHaveValue('6500.00')
    expect(screen.getByLabelText(/activo/i)).toBeChecked()
  })

  it('tipo de producto "Vacuno" muestra el selector de Cuarto; otro tipo lo oculta', async () => {
    const usuario = userEvent.setup()
    render(<FormularioCorte corteExistente={null} onGuardar={vi.fn()} onCancelar={vi.fn()} guardando={false} />)

    expect(screen.getByLabelText(/cuarto/i)).toBeInTheDocument()

    await usuario.selectOptions(screen.getByLabelText(/tipo de producto/i), 'Cerdo')

    expect(screen.queryByLabelText(/cuarto/i)).not.toBeInTheDocument()
  })

  it('con tipo de producto distinto de Vacuno, onGuardar no incluye un cuarto real (lo anula aCorteRequest)', async () => {
    const onGuardar = vi.fn()
    const usuario = userEvent.setup()
    render(<FormularioCorte corteExistente={null} onGuardar={onGuardar} onCancelar={vi.fn()} guardando={false} />)

    await usuario.type(screen.getByLabelText(/^nombre$/i), 'Chorizo')
    await usuario.type(screen.getByLabelText(/^plu$/i), '9002')
    await usuario.selectOptions(screen.getByLabelText(/tipo de producto/i), 'AchurasEmbutidos')
    await usuario.click(screen.getByRole('button', { name: /guardar/i }))

    expect(onGuardar).toHaveBeenCalledWith(expect.objectContaining({ tipoProducto: 'AchurasEmbutidos' }))
  })
})
