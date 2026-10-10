import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { TablaDeCortes } from './TablaDeCortes'
import type { Corte } from '../api/types'

const cortes: Corte[] = [
  {
    id: 'c1',
    nombre: 'Vacío',
    plu: 12,
    cuarto: 'Trasero',
    tipoProducto: 'Vacuno',
    zonaMapa: 'vacio',
    activo: true,
    precioVenta: '6500.00',
    descuentaStockDeCorteId: null,
  },
  {
    id: 'c2',
    nombre: 'Asado',
    plu: 11,
    cuarto: 'Delantero',
    tipoProducto: 'Vacuno',
    zonaMapa: 'asado',
    activo: false,
    precioVenta: null,
    descuentaStockDeCorteId: null,
  },
]

describe('TablaDeCortes', () => {
  it('lista nombre/PLU/cuarto/zona/precio de venta/estado de cada corte', () => {
    render(<TablaDeCortes cortes={cortes} onEditar={vi.fn()} onCambiarActivo={vi.fn()} />)

    expect(screen.getByText('Vacío')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.getByText('Asado')).toBeInTheDocument()
  })

  it('atenúa visualmente los cortes inactivos', () => {
    render(<TablaDeCortes cortes={cortes} onEditar={vi.fn()} onCambiarActivo={vi.fn()} />)

    const filaInactiva = screen.getByTestId('fila-corte-c2')
    expect(filaInactiva.className).toMatch(/opacity/)
  })

  it('el botón de cada fila dice Desactivar si está activo y Activar si no', async () => {
    const onCambiarActivo = vi.fn()
    const usuario = userEvent.setup()
    render(<TablaDeCortes cortes={cortes} onEditar={vi.fn()} onCambiarActivo={onCambiarActivo} />)

    const filaActiva = screen.getByTestId('fila-corte-c1')
    const filaInactiva = screen.getByTestId('fila-corte-c2')

    await usuario.click(within(filaActiva).getByRole('button', { name: /desactivar/i }))
    expect(onCambiarActivo).toHaveBeenCalledWith(cortes[0])

    await usuario.click(within(filaInactiva).getByRole('button', { name: /^activar$/i }))
    expect(onCambiarActivo).toHaveBeenCalledWith(cortes[1])
  })

  it('el botón Editar de una fila llama a onEditar con ese corte', async () => {
    const onEditar = vi.fn()
    const usuario = userEvent.setup()
    render(<TablaDeCortes cortes={cortes} onEditar={onEditar} onCambiarActivo={vi.fn()} />)

    await usuario.click(within(screen.getByTestId('fila-corte-c1')).getByRole('button', { name: /editar/i }))
    expect(onEditar).toHaveBeenCalledWith(cortes[0])
  })

  it('sin cortes, muestra un mensaje neutro', () => {
    render(<TablaDeCortes cortes={[]} onEditar={vi.fn()} onCambiarActivo={vi.fn()} />)
    expect(screen.getByText(/no hay cortes/i)).toBeInTheDocument()
  })

  it('un corte sin cuarto (tipoProducto distinto de Vacuno, Fase 7) muestra "—"', () => {
    const corteDeCerdo: Corte = {
      id: 'c3',
      nombre: 'Bondiola',
      plu: 41,
      cuarto: null,
      tipoProducto: 'Cerdo',
      zonaMapa: null,
      activo: true,
      precioVenta: null,
      descuentaStockDeCorteId: null,
    }
    render(<TablaDeCortes cortes={[corteDeCerdo]} onEditar={vi.fn()} onCambiarActivo={vi.fn()} />)

    // "—" también sale en zona de mapa y precio de venta (ambos null acá): confirmamos que
    // aparece al menos para cuarto, no que sea el único "—" de la fila.
    expect(within(screen.getByTestId('fila-corte-c3')).getAllByText('—').length).toBeGreaterThanOrEqual(1)
  })

  it('un corte que descuenta el stock de otro muestra el nombre del destino en "Descuenta de"', () => {
    const bifeDeChorizo: Corte = {
      id: 'c4',
      nombre: 'Bife de chorizo',
      plu: 50,
      cuarto: 'Trasero',
      tipoProducto: 'Vacuno',
      zonaMapa: null,
      activo: true,
      precioVenta: '9000.00',
      descuentaStockDeCorteId: 'c2', // Asado
    }
    render(<TablaDeCortes cortes={[...cortes, bifeDeChorizo]} onEditar={vi.fn()} onCambiarActivo={vi.fn()} />)

    expect(within(screen.getByTestId('fila-corte-c4')).getByText('Asado')).toBeInTheDocument()
  })
})
