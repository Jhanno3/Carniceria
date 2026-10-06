import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { VentasDeHoy } from './VentasDeHoy'
import { useAnularVenta } from '../api/useAnularVenta'
import type { VentaResponse } from '../api/types'

vi.mock('../api/useAnularVenta')

const useAnularVentaMock = vi.mocked(useAnularVenta)

const USUARIO_ACTUAL = 'u1'
const OTRO_USUARIO = 'u2'
const DOS_MINUTOS_MS = 2 * 60 * 1000
const DIEZ_MINUTOS_MS = 10 * 60 * 1000

function venta(
  id: string,
  hora: string,
  corteNombre: string,
  opciones: Partial<Pick<VentaResponse, 'anulada' | 'usuarioId' | 'fechaHora'>> = {},
): VentaResponse {
  return {
    id,
    fechaHora: `2026-10-10T${hora}:00-03:00`,
    corteId: 'c1',
    corteNombre,
    kg: '1.000',
    precioTotal: null,
    codigoLeido: '',
    anulada: false,
    usuarioId: USUARIO_ACTUAL,
    ...opciones,
  }
}

describe('VentasDeHoy', () => {
  let mutate: ReturnType<typeof vi.fn>

  beforeEach(() => {
    mutate = vi.fn()
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useAnularVentaMock.mockReturnValue({ mutate, isPending: false } as any)
  })

  function renderizar(ventas: VentaResponse[], puedeAnularCualquiera = false) {
    return render(
      <VentasDeHoy ventas={ventas} usuarioActualId={USUARIO_ACTUAL} puedeAnularCualquiera={puedeAnularCualquiera} />,
    )
  }

  it('sin ventas, muestra un mensaje neutro', () => {
    renderizar([])
    expect(screen.getByText(/todavía no hay ventas/i)).toBeInTheDocument()
  })

  it('lista las ventas en el orden recibido (más reciente primero)', () => {
    const ventas = [venta('3', '15:00', 'Asado'), venta('2', '14:00', 'Vacío'), venta('1', '13:00', 'Paleta')]
    renderizar(ventas)

    const nombres = screen.getAllByText(/Asado|Vacío|Paleta/).map((el) => el.textContent)
    expect(nombres).toEqual(['Asado', 'Vacío', 'Paleta'])
  })

  it('muestra como mucho 5 al principio y "Ver todas" las revela', async () => {
    const ventas = Array.from({ length: 7 }, (_, i) => venta(String(i), `1${i}:00`, `Corte ${i}`))
    renderizar(ventas)

    expect(screen.queryByText('Corte 6')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /ver todas/i })).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /ver todas/i }))

    expect(screen.getByText('Corte 6')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /ver todas/i })).not.toBeInTheDocument()
  })

  it('el dueño ve "Anular" en cualquier venta no anulada, aunque sea vieja y ajena', () => {
    const vieja = venta('1', '10:00', 'Vacío', {
      usuarioId: OTRO_USUARIO,
      fechaHora: new Date(Date.now() - DIEZ_MINUTOS_MS).toISOString(),
    })
    renderizar([vieja], true)

    expect(screen.getByRole('button', { name: /anular/i })).toBeInTheDocument()
  })

  it('un empleado ve "Anular" en una venta propia y reciente', () => {
    const propiaYReciente = venta('1', '10:00', 'Vacío', {
      usuarioId: USUARIO_ACTUAL,
      fechaHora: new Date(Date.now() - DOS_MINUTOS_MS).toISOString(),
    })
    renderizar([propiaYReciente], false)

    expect(screen.getByRole('button', { name: /anular/i })).toBeInTheDocument()
  })

  it('un empleado NO ve "Anular" en una venta ajena', () => {
    const ajena = venta('1', '10:00', 'Vacío', {
      usuarioId: OTRO_USUARIO,
      fechaHora: new Date(Date.now() - DOS_MINUTOS_MS).toISOString(),
    })
    renderizar([ajena], false)

    expect(screen.queryByRole('button', { name: /anular/i })).not.toBeInTheDocument()
  })

  it('un empleado NO ve "Anular" en una venta propia de más de 5 minutos', () => {
    const propiaYVieja = venta('1', '10:00', 'Vacío', {
      usuarioId: USUARIO_ACTUAL,
      fechaHora: new Date(Date.now() - DIEZ_MINUTOS_MS).toISOString(),
    })
    renderizar([propiaYVieja], false)

    expect(screen.queryByRole('button', { name: /anular/i })).not.toBeInTheDocument()
  })

  it('una venta ya anulada se muestra distinguible (no solo por color) y sin botón', () => {
    const anulada = venta('1', '10:00', 'Vacío', { anulada: true })
    renderizar([anulada], true)

    expect(screen.getByText(/anulada/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /anular/i })).not.toBeInTheDocument()
  })

  it('click en "Anular" dispara la mutación con el id de la venta', async () => {
    const propiaYReciente = venta('venta-1', '10:00', 'Vacío', {
      fechaHora: new Date(Date.now() - DOS_MINUTOS_MS).toISOString(),
    })
    renderizar([propiaYReciente], true)

    await userEvent.click(screen.getByRole('button', { name: /anular/i }))

    expect(mutate).toHaveBeenCalledWith('venta-1')
  })
})
