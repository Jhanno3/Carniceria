import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { UltimoEscaneo } from './UltimoEscaneo'

describe('UltimoEscaneo', () => {
  it('sin ningún escaneo, muestra un mensaje neutro', () => {
    render(<UltimoEscaneo resultado={null} />)
    expect(screen.getByText(/todavía no escaneaste/i)).toBeInTheDocument()
  })

  it('con éxito, muestra "Descontado del stock" en verde con el corte y el peso', () => {
    render(<UltimoEscaneo resultado={{ tipo: 'exito', corteNombre: 'Vacío', kg: 1.5, pendiente: false }} />)
    expect(screen.getByText('Descontado del stock').parentElement).toHaveClass('text-exito-texto')
    expect(screen.getByText(/Vacío/)).toBeInTheDocument()
    expect(screen.getByText(/1,5 kg/)).toBeInTheDocument()
  })

  it('pendiente de sincronizar, muestra el aviso en vez de "Descontado del stock"', () => {
    render(<UltimoEscaneo resultado={{ tipo: 'exito', corteNombre: 'Vacío', kg: 1.5, pendiente: true }} />)
    expect(screen.getByText('Pendiente de sincronizar')).toBeInTheDocument()
    expect(screen.queryByText('Descontado del stock')).not.toBeInTheDocument()
  })

  it.each([
    ['DIGITO_VERIFICADOR_INVALIDO', /código escaneado no es válido/i],
    ['PREFIJO_INVALIDO', /peso variable/i],
    ['PLU_INEXISTENTE', /ningún corte activo/i],
    ['PESO_CERO', /peso leído es cero/i],
  ])('con el error %s, muestra el motivo en rojo', (codigo, patron) => {
    render(<UltimoEscaneo resultado={{ tipo: 'error', codigo, mensaje: 'mensaje crudo' }} />)
    const mensaje = screen.getByText(patron)
    expect(mensaje).toHaveClass('text-error')
  })
})
