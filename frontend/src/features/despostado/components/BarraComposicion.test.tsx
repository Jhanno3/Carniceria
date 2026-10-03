import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { BarraComposicion } from './BarraComposicion'

describe('BarraComposicion', () => {
  it('cuando cuadra (diferencia ≤ 0,05 kg), el mensaje es verde', () => {
    render(<BarraComposicion pesoKg={100} vendibleKg={81} hueso={11} grasa={6} merma={2} sinAsignarKg={0} />)

    expect(screen.getByText(/cuadra con el peso de entrada/i)).toBeInTheDocument()
  })

  it('cuando faltan kilos, el mensaje es naranja y dice cuánto falta', () => {
    render(<BarraComposicion pesoKg={100} vendibleKg={79} hueso={11} grasa={6} merma={2} sinAsignarKg={2} />)

    expect(screen.getByText(/faltan asignar 2,0 kg/i)).toBeInTheDocument()
  })

  it('cuando sobran kilos, el mensaje es rojo y dice cuánto sobra', () => {
    render(<BarraComposicion pesoKg={100} vendibleKg={83} hueso={11} grasa={6} merma={2} sinAsignarKg={-2} />)

    expect(screen.getByText(/sobran 2,0 kg: revisá las pesadas/i)).toBeInTheDocument()
  })
})
