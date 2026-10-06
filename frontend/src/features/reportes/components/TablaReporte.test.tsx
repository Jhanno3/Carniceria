import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { TablaReporte } from './TablaReporte'

describe('TablaReporte', () => {
  it('sin filas, muestra un mensaje neutro (spec.md 5.3: no una tabla vacía sin explicación)', () => {
    render(<TablaReporte etiquetaGrupo="Proveedor" etiquetaSinValor="Sin proveedor" filas={[]} />)
    expect(screen.getByText(/no hay datos/i)).toBeInTheDocument()
  })

  it('muestra una fila por grupo con cantidad, rendimiento y costo', () => {
    render(
      <TablaReporte
        etiquetaGrupo="Proveedor"
        etiquetaSinValor="Sin proveedor"
        filas={[
          {
            grupo: 'Frigorífico Sur',
            cantidadEntradas: 3,
            rendimientoPromedioPorc: '86.36',
            costoKgVendiblePromedio: '1263',
          },
        ]}
      />,
    )

    expect(screen.getByText('Frigorífico Sur')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
    expect(screen.getByText(/86,36 %/)).toBeInTheDocument()
    expect(screen.getByText(/\$ 1\.263/)).toBeInTheDocument()
  })

  it('un grupo sin valor (null) se muestra con la etiqueta de "sin valor", y el costo null como "—"', () => {
    render(
      <TablaReporte
        etiquetaGrupo="Categoría"
        etiquetaSinValor="Sin categoría"
        filas={[{ grupo: null, cantidadEntradas: 1, rendimientoPromedioPorc: '75.00', costoKgVendiblePromedio: null }]}
      />,
    )

    expect(screen.getByText('Sin categoría')).toBeInTheDocument()
    expect(screen.getByText('—')).toBeInTheDocument()
  })
})
