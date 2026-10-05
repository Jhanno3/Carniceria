import { useEffect, useRef, useState } from 'react'
import type { KeyboardEvent } from 'react'
import { useEscanear } from '../api/useEscanear'
import { ApiError } from '../../../shared/api/apiFetch'
import type { VentaResponse } from '../api/types'

export type ResultadoEscaneo =
  | { tipo: 'exito'; venta: VentaResponse }
  | { tipo: 'error'; codigo: string; mensaje: string }

interface CampoEscaneoProps {
  onResultado: (resultado: ResultadoEscaneo) => void
}

/**
 * FR-201/Principio IV: el campo mantiene el foco todo el tiempo que la pantalla está
 * abierta. El lector USB "escribe" el código como si fuera un teclado y manda Enter al
 * final — por eso es un input de texto común, sin ninguna API de hardware.
 */
export function CampoEscaneo({ onResultado }: CampoEscaneoProps) {
  const [valor, setValor] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)
  const escanear = useEscanear()

  function enfocar() {
    inputRef.current?.focus()
  }

  useEffect(() => {
    enfocar()
    // setTimeout, no onBlur directo: así un click en otro botón de la pantalla (ej.
    // "ver todas" de Ventas de hoy) alcanza a disparar su propio onClick antes de que
    // el foco vuelva acá (US-2.1: "si hago click en cualquier otro lado, el foco vuelve").
    function alHacerClickEnCualquierLado() {
      setTimeout(enfocar, 0)
    }
    document.addEventListener('click', alHacerClickEnCualquierLado)
    return () => document.removeEventListener('click', alHacerClickEnCualquierLado)
  }, [])

  function alPresionarTecla(evento: KeyboardEvent<HTMLInputElement>) {
    if (evento.key !== 'Enter') return
    const codigo = valor.trim()
    setValor('')
    if (codigo === '') return

    escanear.mutate(
      { codigo, idClienteLocal: crypto.randomUUID() },
      {
        onSuccess: (venta) => {
          onResultado({ tipo: 'exito', venta })
          enfocar()
        },
        onError: (error) => {
          const esApiError = error instanceof ApiError
          onResultado({
            tipo: 'error',
            codigo: esApiError ? error.codigo : 'ERROR_DESCONOCIDO',
            mensaje: esApiError ? error.message : 'No pudimos registrar el escaneo.',
          })
          enfocar()
        },
      },
    )
  }

  return (
    <div className="flex flex-col gap-2 rounded-xl border-2 border-borde p-4">
      <label htmlFor="campo-escaneo" className="text-etiqueta font-medium uppercase text-texto-secundario">
        Escaneá la etiqueta de la balanza
      </label>
      <input
        id="campo-escaneo"
        ref={inputRef}
        type="text"
        autoComplete="off"
        value={valor}
        onChange={(e) => setValor(e.target.value)}
        onKeyDown={alPresionarTecla}
        className="h-14 w-full rounded-xl border border-borde-campo px-4 text-cuerpo"
      />
    </div>
  )
}
