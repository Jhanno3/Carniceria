import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { MapaCortes } from './MapaCortes'

describe('MapaCortes', () => {
  it('muestra la aclaración fija sobre carne picada y recortes', () => {
    render(<MapaCortes zonas={[]} zonaResaltada={null} detalle={null} />)

    expect(screen.getByText(/carne picada y recortes no tienen zona en el mapa/i)).toBeInTheDocument()
  })

  it('la zona más pesada queda con el color más oscuro de la escala', () => {
    render(
      <MapaCortes
        zonas={[
          { id: 'asado', kg: 11 },
          { id: 'vacio', kg: 3.3 },
        ]}
        zonaResaltada={null}
        detalle={null}
      />,
    )

    expect(screen.getByTestId('zona-asado')).toHaveAttribute('fill', 'rgb(110, 20, 20)')
  })

  it('la zona resaltada tiene el borde grueso y se ve el detalle del corte seleccionado', () => {
    render(
      <MapaCortes
        zonas={[{ id: 'vacio', kg: 3.3 }]}
        zonaResaltada="vacio"
        detalle={{ nombre: 'Vacío', kg: 3.3, porcentajeDeLaMediaRes: 3.3 }}
      />,
    )

    expect(screen.getByTestId('zona-vacio')).toHaveAttribute('stroke-width', '3')
    expect(screen.getByText('Vacío · 3,3 kg · 3,3 % de la media res')).toBeInTheDocument()
  })

  it('sin ningún corte cargado, no rompe y no resalta nada', () => {
    render(<MapaCortes zonas={[]} zonaResaltada={null} detalle={null} />)

    expect(screen.queryByText(/% de la media res/)).not.toBeInTheDocument()
  })
})
