import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { UsuariosPage } from './UsuariosPage'
import { usePerfilesTodos } from './api/usePerfilesTodos'
import { useActualizarPerfil } from './api/useActualizarPerfil'
import { usePerfilPropio } from './api/usePerfilPropio'
import type { PerfilResponse } from './api/types'

vi.mock('./api/usePerfilesTodos')
vi.mock('./api/useActualizarPerfil')
vi.mock('./api/usePerfilPropio')

const usePerfilesTodosMock = vi.mocked(usePerfilesTodos)
const useActualizarPerfilMock = vi.mocked(useActualizarPerfil)
const usePerfilPropioMock = vi.mocked(usePerfilPropio)

const ADMIN_ID = 'admin-1'

function perfil(
  id: string, nombre: string, rol: PerfilResponse['rol'], estado: PerfilResponse['estado'],
): PerfilResponse {
  return { id, nombre, rol, estado, duenoId: rol === 'dueno' ? id : null }
}

describe('UsuariosPage', () => {
  let mutate: ReturnType<typeof vi.fn>

  beforeEach(() => {
    mutate = vi.fn()
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    useActualizarPerfilMock.mockReturnValue({ mutate, isPending: false } as any)
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    usePerfilPropioMock.mockReturnValue({ data: perfil(ADMIN_ID, 'Yo, admin', 'admin', 'aprobado') } as any)
  })

  function mockearTodas(perfiles: PerfilResponse[]) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    usePerfilesTodosMock.mockReturnValue({ data: perfiles, isLoading: false } as any)
  }

  it('la sección de pendientes conserva los botones Aprobar/Rechazar', () => {
    mockearTodas([perfil('p1', 'Pendiente', 'empleado', 'pendiente')])

    render(<UsuariosPage />)

    expect(screen.getByRole('button', { name: /aprobar/i })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /rechazar/i })).toBeInTheDocument()
  })

  it('la tabla "Todas las cuentas" lista cuentas de cualquier estado', () => {
    mockearTodas([
      perfil('p1', 'Aprobada', 'dueno', 'aprobado'),
      perfil('p2', 'Rechazada', 'empleado', 'rechazado'),
    ])

    render(<UsuariosPage />)

    expect(screen.getByText('Aprobada')).toBeInTheDocument()
    expect(screen.getByText('Rechazada')).toBeInTheDocument()
  })

  it('una cuenta aprobada muestra "Pausar", que manda el mismo rol con estado pausado', async () => {
    mockearTodas([perfil('p1', 'Activa', 'empleado', 'aprobado')])
    render(<UsuariosPage />)

    await userEvent.click(screen.getByRole('button', { name: /pausar/i }))

    expect(mutate).toHaveBeenCalledWith({ id: 'p1', rol: 'empleado', estado: 'pausado' })
  })

  it('una cuenta pausada muestra "Reactivar" en vez de "Pausar"', async () => {
    mockearTodas([perfil('p1', 'Pausada', 'empleado', 'pausado')])
    render(<UsuariosPage />)

    expect(screen.queryByRole('button', { name: /^pausar$/i })).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /reactivar/i }))

    expect(mutate).toHaveBeenCalledWith({ id: 'p1', rol: 'empleado', estado: 'aprobado' })
  })

  it('la propia cuenta del admin logueado no muestra ningún botón de Pausar/Reactivar', () => {
    mockearTodas([perfil(ADMIN_ID, 'Yo, admin', 'admin', 'aprobado')])

    render(<UsuariosPage />)

    expect(screen.queryByRole('button', { name: /pausar/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /reactivar/i })).not.toBeInTheDocument()
  })
})
