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
        gananciaEstimada={null}
      />,
    )

    // Costo por kg vendible Y Ganancia estimada muestran "—" las dos (ninguna se puede calcular).
    expect(screen.getAllByText('—')).toHaveLength(2)
    expect(screen.getAllByText(/cargá el precio de compra/i)).toHaveLength(2)
  })

  it('con precio de compra, muestra el costo por kg vendible formateado es-AR', () => {
    render(
      <TarjetasResumen
        pesoKgTexto="100"
        onCambiarPesoKg={() => {}}
        precioKgTexto="5200"
        onCambiarPrecioKg={() => {}}
        resumen={{ ...resumenBase, costoTotal: 520000, costoKgVendible: 6420 }}
        gananciaEstimada={null}
      />,
    )

    expect(screen.getByText('$ 6.420')).toBeInTheDocument()
    expect(screen.getByText('81,0 kg')).toBeInTheDocument()
    expect(screen.getByText('19,0 kg')).toBeInTheDocument()
  })

  it('sin costo por kg vendible, la ganancia estimada muestra "—" y el texto de ayuda', () => {
    render(
      <TarjetasResumen
        pesoKgTexto="100"
        onCambiarPesoKg={() => {}}
        precioKgTexto=""
        onCambiarPrecioKg={() => {}}
        resumen={resumenBase}
        gananciaEstimada={null}
      />,
    )

    expect(screen.getAllByText(/cargá el precio de compra/i)).toHaveLength(2)
  })

  it('con costo pero sin ningún corte con precio de venta, pide cargar precios en Editar cortes', () => {
    render(
      <TarjetasResumen
        pesoKgTexto="100"
        onCambiarPesoKg={() => {}}
        precioKgTexto="5200"
        onCambiarPrecioKg={() => {}}
        resumen={{ ...resumenBase, costoTotal: 520000, costoKgVendible: 6420 }}
        gananciaEstimada={null}
      />,
    )

    expect(screen.getByText(/editar cortes/i)).toBeInTheDocument()
  })

  it('con ganancia estimada positiva, la muestra en verde (no el rojo de "vendible")', () => {
    render(
      <TarjetasResumen
        pesoKgTexto="100"
        onCambiarPesoKg={() => {}}
        precioKgTexto="5200"
        onCambiarPrecioKg={() => {}}
        resumen={{ ...resumenBase, costoTotal: 520000, costoKgVendible: 6420 }}
        gananciaEstimada={24970}
      />,
    )

    const cifra = screen.getByText('$ 24.970')
    expect(cifra).toHaveClass('text-exito-texto')
    expect(cifra).toHaveClass('text-cifra-tarjeta')
  })

  it('con ganancia estimada negativa, la muestra en rojo de error', () => {
    render(
      <TarjetasResumen
        pesoKgTexto="100"
        onCambiarPesoKg={() => {}}
        precioKgTexto="5200"
        onCambiarPrecioKg={() => {}}
        resumen={{ ...resumenBase, costoTotal: 520000, costoKgVendible: 6420 }}
        gananciaEstimada={-1500}
      />,
    )

    expect(screen.getByText('$ -1.500')).toHaveClass('text-error')
  })

  it('con ganancia estimada de más de 6 cifras, usa el tamaño chico y no corta de línea', () => {
    render(
      <TarjetasResumen
        pesoKgTexto="100"
        onCambiarPesoKg={() => {}}
        precioKgTexto="5200"
        onCambiarPrecioKg={() => {}}
        resumen={{ ...resumenBase, costoTotal: 520000, costoKgVendible: 6420 }}
        gananciaEstimada={1234567}
      />,
    )

    const cifra = screen.getByText('$ 1.234.567')
    expect(cifra).toHaveClass('text-cifra-tarjeta-chica')
    expect(cifra).toHaveClass('whitespace-nowrap')
    expect(cifra).not.toHaveClass('text-cifra-tarjeta')
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
        gananciaEstimada={null}
      />,
    )

    await userEvent.type(screen.getByLabelText(/peso media res/i), '1')

    expect(onCambiarPesoKg).toHaveBeenCalledWith('1')
  })
})
