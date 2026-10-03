import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { TarjetasResumen } from './TarjetasResumen'

const resumenBase = {
  vendibleKg: 81,
  perdidaKg: 19,
  sinAsignarKg: 0,
  rendimientoPorc: 81,
  costoTotal: null as number | null,
  costoKgVendible: null as number | null,
}

describe('TarjetasResumen', () => {
  it('sin precio de compra, muestra "—" y el texto de ayuda', () => {
    render(
      <TarjetasResumen
        pesoKgTexto="100"
        onCambiarPesoKg={() => {}}
        precioKgTexto=""
        onCambiarPrecioKg={() => {}}
        resumen={resumenBase}
      />,
    )

    expect(screen.getByText('—')).toBeInTheDocument()
    expect(screen.getByText(/cargá el precio de compra/i)).toBeInTheDocument()
  })

  it('con precio de compra, muestra el costo por kg vendible formateado es-AR', () => {
    render(
      <TarjetasResumen
        pesoKgTexto="100"
        onCambiarPesoKg={() => {}}
        precioKgTexto="5200"
        onCambiarPrecioKg={() => {}}
        resumen={{ ...resumenBase, costoTotal: 520000, costoKgVendible: 6420 }}
      />,
    )

    expect(screen.getByText('$ 6.420')).toBeInTheDocument()
    expect(screen.getByText('81,0 kg')).toBeInTheDocument()
    expect(screen.getByText('19,0 kg')).toBeInTheDocument()
  })

  it('escribir en el campo de peso llama a onCambiarPesoKg', async () => {
    const onCambiarPesoKg = vi.fn()
    render(
      <TarjetasResumen
        pesoKgTexto=""
        onCambiarPesoKg={onCambiarPesoKg}
        precioKgTexto=""
        onCambiarPrecioKg={() => {}}
        resumen={resumenBase}
      />,
    )

    await userEvent.type(screen.getByLabelText(/peso media res/i), '1')

    expect(onCambiarPesoKg).toHaveBeenCalledWith('1')
  })
})
