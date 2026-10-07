import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { TablaCortes } from './TablaCortes'
import type { CorteResponse } from '../api/types'

const cortes: CorteResponse[] = [
  {
    id: 'c1',
    nombre: 'Vacío',
    plu: 12,
    cuarto: 'Trasero',
    tipoProducto: 'Vacuno',
    zonaMapa: 'vacio',
    activo: true,
    precioVenta: null,
  },
  {
    id: 'c2',
    nombre: 'Asado',
    plu: 11,
    cuarto: 'Delantero',
    tipoProducto: 'Vacuno',
    zonaMapa: 'asado',
    activo: true,
    precioVenta: null,
  },
]

describe('TablaCortes', () => {
  it('editar el campo de kilos de un corte llama a onCambiarKg', async () => {
    const onCambiarKg = vi.fn()
    render(
      <TablaCortes
        cortes={cortes}
        kgPorCorte={{}}
        onCambiarKg={onCambiarKg}
        corteSeleccionadoId={null}
        onSeleccionarCorte={() => {}}
      />,
    )

    await userEvent.type(screen.getByLabelText(/kilos de vacío/i), '3')

    expect(onCambiarKg).toHaveBeenCalledWith('c1', '3')
  })

  it('tocar el nombre de un corte lo selecciona', async () => {
    const onSeleccionarCorte = vi.fn()
    render(
      <TablaCortes
        cortes={cortes}
        kgPorCorte={{}}
        onCambiarKg={() => {}}
        corteSeleccionadoId={null}
        onSeleccionarCorte={onSeleccionarCorte}
      />,
    )

    await userEvent.click(screen.getByRole('button', { name: 'Asado' }))

    expect(onSeleccionarCorte).toHaveBeenCalledWith('c2')
  })

  it('la fila seleccionada se resalta', () => {
    render(
      <TablaCortes
        cortes={cortes}
        kgPorCorte={{}}
        onCambiarKg={() => {}}
        corteSeleccionadoId="c1"
        onSeleccionarCorte={() => {}}
      />,
    )

    expect(screen.getByTestId('fila-corte-c1')).toHaveClass('bg-rosado')
    expect(screen.getByTestId('fila-corte-c2')).not.toHaveClass('bg-rosado')
  })

  it('sin corteNombresHabilitados (o null), ningún corte se atenúa (Fase 6: "Media res")', () => {
    render(
      <TablaCortes
        cortes={cortes}
        kgPorCorte={{}}
        onCambiarKg={() => {}}
        corteSeleccionadoId={null}
        onSeleccionarCorte={() => {}}
        corteNombresHabilitados={null}
      />,
    )

    expect(screen.getByTestId('fila-corte-c1').className).not.toMatch(/opacity/)
    expect(screen.getByLabelText(/kilos de vacío/i)).not.toBeDisabled()
  })

  it('con corteNombresHabilitados, atenúa y deshabilita los cortes fuera del set (Fase 6)', () => {
    render(
      <TablaCortes
        cortes={cortes}
        kgPorCorte={{}}
        onCambiarKg={() => {}}
        corteSeleccionadoId={null}
        onSeleccionarCorte={() => {}}
        corteNombresHabilitados={new Set(['Asado'])}
      />,
    )

    expect(screen.getByTestId('fila-corte-c1').className).toMatch(/opacity/) // Vacío: no habilitado
    expect(screen.getByLabelText(/kilos de vacío/i)).toBeDisabled()
    expect(screen.getByTestId('fila-corte-c2').className).not.toMatch(/opacity/) // Asado: habilitado
    expect(screen.getByLabelText(/kilos de asado/i)).not.toBeDisabled()
  })

  it('un corte atenuado igual se puede seleccionar (solo se bloquea cargar kg, no la lectura)', async () => {
    const onSeleccionarCorte = vi.fn()
    render(
      <TablaCortes
        cortes={cortes}
        kgPorCorte={{}}
        onCambiarKg={() => {}}
        corteSeleccionadoId={null}
        onSeleccionarCorte={onSeleccionarCorte}
        corteNombresHabilitados={new Set(['Asado'])}
      />,
    )

    await userEvent.click(screen.getByRole('button', { name: 'Vacío' }))

    expect(onSeleccionarCorte).toHaveBeenCalledWith('c1')
  })

  it('un corte sin cuarto (tipoProducto distinto de Vacuno, Fase 7) muestra "—" en esa columna', () => {
    const corteDeCerdo: CorteResponse = {
      id: 'c3',
      nombre: 'Bondiola',
      plu: 41,
      cuarto: null,
      tipoProducto: 'Cerdo',
      zonaMapa: null,
      activo: true,
      precioVenta: null,
    }
    render(
      <TablaCortes
        cortes={[corteDeCerdo]}
        kgPorCorte={{}}
        onCambiarKg={() => {}}
        corteSeleccionadoId={null}
        onSeleccionarCorte={() => {}}
      />,
    )

    expect(screen.getByText('—')).toBeInTheDocument()
  })
})
