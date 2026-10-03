import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { TablaCortes } from './TablaCortes'
import type { CorteResponse } from '../api/types'

const cortes: CorteResponse[] = [
  { id: 'c1', nombre: 'Vacío', plu: 12, cuarto: 'Trasero', zonaMapa: 'vacio', activo: true },
  { id: 'c2', nombre: 'Asado', plu: 11, cuarto: 'Delantero', zonaMapa: 'asado', activo: true },
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
})
