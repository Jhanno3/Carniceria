import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CampoEscaneo } from './CampoEscaneo'
import { useEscanear } from '../api/useEscanear'
import { ApiError } from '../../../shared/api/apiFetch'
import type { VentaResponse } from '../api/types'

vi.mock('../api/useEscanear')

const useEscanearMock = vi.mocked(useEscanear)

const ventaDeEjemplo: VentaResponse = {
  id: '1',
  fechaHora: '2026-10-10T10:00:00-03:00',
  corteId: 'c1',
  corteNombre: 'Vacío',
  kg: '1.250',
  codigoLeido: '2000012012501',
  anulada: false,
}

describe('CampoEscaneo', () => {
  let mutate: ReturnType<typeof vi.fn>

  beforeEach(() => {
    mutate = vi.fn()
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useEscanearMock.mockReturnValue({ mutate } as any)
  })

  function campo() {
    return screen.getByLabelText(/escaneá la etiqueta/i) as HTMLInputElement
  }

  it('tiene el foco apenas se monta', () => {
    render(<CampoEscaneo onResultado={() => {}} />)
    expect(campo()).toHaveFocus()
  })

  it('Enter dispara el escaneo con el valor acumulado y limpia el campo', async () => {
    const usuario = userEvent.setup()
    render(<CampoEscaneo onResultado={() => {}} />)
    const input = campo()

    await usuario.type(input, '2000012012501{Enter}')

    expect(mutate).toHaveBeenCalledWith(
      expect.objectContaining({ codigo: '2000012012501' }),
      expect.anything(),
    )
    expect(input.value).toBe('')
  })

  it('mantiene el foco después de un escaneo exitoso', async () => {
    mutate.mockImplementation((_req, { onSuccess }) => onSuccess(ventaDeEjemplo))
    const onResultado = vi.fn()
    const usuario = userEvent.setup()
    render(<CampoEscaneo onResultado={onResultado} />)
    const input = campo()

    await usuario.type(input, '2000012012501{Enter}')

    expect(onResultado).toHaveBeenCalledWith({ tipo: 'exito', venta: ventaDeEjemplo })
    await waitFor(() => expect(input).toHaveFocus())
  })

  it('mantiene el foco después de un error', async () => {
    mutate.mockImplementation((_req, { onError }) =>
      onError(new ApiError('PLU_INEXISTENTE', 'El código no corresponde a ningún corte activo.', 400)),
    )
    const onResultado = vi.fn()
    const usuario = userEvent.setup()
    render(<CampoEscaneo onResultado={onResultado} />)
    const input = campo()

    await usuario.type(input, '2000099010001{Enter}')

    expect(onResultado).toHaveBeenCalledWith({
      tipo: 'error',
      codigo: 'PLU_INEXISTENTE',
      mensaje: 'El código no corresponde a ningún corte activo.',
    })
    await waitFor(() => expect(input).toHaveFocus())
  })

  it('vuelve a enfocar después de un click en cualquier otro lado de la pantalla', async () => {
    render(
      <div>
        <CampoEscaneo onResultado={() => {}} />
        <button type="button">Otro elemento</button>
      </div>,
    )
    const input = campo()
    const boton = screen.getByRole('button', { name: 'Otro elemento' })

    const usuario = userEvent.setup()
    await usuario.click(boton)

    await waitFor(() => expect(input).toHaveFocus())
  })
})
