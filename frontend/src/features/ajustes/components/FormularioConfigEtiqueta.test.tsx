import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FormularioConfigEtiqueta } from './FormularioConfigEtiqueta'
import { useConfigEtiqueta } from '../api/useConfigEtiqueta'
import { useActualizarConfigEtiqueta } from '../api/useActualizarConfigEtiqueta'
import { ApiError } from '../../../shared/api/apiFetch'
import type { ConfigEtiquetaDto } from '../api/types'

vi.mock('../api/useConfigEtiqueta')
vi.mock('../api/useActualizarConfigEtiqueta')

const useConfigEtiquetaMock = vi.mocked(useConfigEtiqueta)
const useActualizarConfigEtiquetaMock = vi.mocked(useActualizarConfigEtiqueta)

const configDeEjemplo: ConfigEtiquetaDto = {
  prefijoDesde: 20,
  prefijoHasta: 29,
  inicioPlu: 2,
  largoPlu: 5,
  inicioValor: 7,
  largoValor: 5,
  tipoValor: 'peso',
  decimales: 3,
}

describe('FormularioConfigEtiqueta', () => {
  let mutateAsync: ReturnType<typeof vi.fn>

  beforeEach(() => {
    mutateAsync = vi.fn()
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useConfigEtiquetaMock.mockReturnValue({ data: configDeEjemplo, isLoading: false } as any)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useActualizarConfigEtiquetaMock.mockReturnValue({ mutateAsync, isPending: false } as any)
  })

  it('carga los valores actuales', () => {
    render(<FormularioConfigEtiqueta />)
    expect(screen.getByLabelText(/prefijo desde/i)).toHaveValue(20)
    expect(screen.getByLabelText(/largo del plu/i)).toHaveValue(5)
    expect(screen.getByLabelText(/tipo de valor/i)).toHaveValue('peso')
  })

  it('guarda los cambios', async () => {
    mutateAsync.mockResolvedValue(configDeEjemplo)
    const usuario = userEvent.setup()
    render(<FormularioConfigEtiqueta />)

    await usuario.clear(screen.getByLabelText(/prefijo desde/i))
    await usuario.type(screen.getByLabelText(/prefijo desde/i), '21')
    await usuario.click(screen.getByRole('button', { name: /guardar/i }))

    await waitFor(() =>
      expect(mutateAsync).toHaveBeenCalledWith(expect.objectContaining({ prefijoDesde: 21 })),
    )
  })

  it('muestra el error de rango superpuesto si el backend lo rechaza', async () => {
    mutateAsync.mockRejectedValue(
      new ApiError('CONFIGURACION_ETIQUETA_INVALIDA', 'Los rangos se superponen.', 400),
    )
    const usuario = userEvent.setup()
    render(<FormularioConfigEtiqueta />)

    await usuario.click(screen.getByRole('button', { name: /guardar/i }))

    expect(await screen.findByText(/se superponen/i)).toBeInTheDocument()
  })
})
