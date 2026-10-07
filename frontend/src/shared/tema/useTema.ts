import { useCallback, useEffect, useState } from 'react'

export type Tema = 'claro' | 'oscuro'

export const CLAVE_TEMA = 'carniceria:tema'

function leerPreferenciaGuardada(): Tema | null {
  try {
    const guardado = localStorage.getItem(CLAVE_TEMA)
    return guardado === 'claro' || guardado === 'oscuro' ? guardado : null
  } catch {
    return null
  }
}

function leerPreferenciaInicial(): Tema {
  const guardado = leerPreferenciaGuardada()
  if (guardado) return guardado
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'oscuro' : 'claro'
}

/** Alterna y persiste el tema claro/oscuro. El flash inicial se evita con el script de index.html, que aplica la misma preferencia antes de que React monte. */
export function useTema() {
  const [tema, setTema] = useState<Tema>(leerPreferenciaInicial)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', tema === 'oscuro')
    try {
      localStorage.setItem(CLAVE_TEMA, tema)
    } catch {
      // Modo privado u otro bloqueo de localStorage: el tema sigue funcionando, solo no persiste.
    }
  }, [tema])

  const alternarTema = useCallback(() => {
    setTema((actual) => (actual === 'oscuro' ? 'claro' : 'oscuro'))
  }, [])

  return { tema, alternarTema }
}
