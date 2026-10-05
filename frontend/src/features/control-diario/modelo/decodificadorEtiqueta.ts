import { validarDigitoVerificador } from './ean13'
import type { ConfigEtiqueta } from './configEtiqueta'

// Espejo de backend/.../escaneo/modelo/DecodificadorEtiqueta.java +
// ResultadoDecodificacion.java (plan-fase3.md, 3.1): el backend sigue siendo la autoridad
// cuando hay conexión — esto solo entra en juego para decidir qué encolar y qué mostrar
// mientras no la hay (Bloque 7). No conoce `cortes`: si el PLU existe y está activo lo
// valida quien use este decodificador, con el catálogo ya cacheado.
export type ResultadoDecodificacion =
  | { tipo: 'Exito'; plu: number; kg: number }
  | { tipo: 'DigitoVerificadorInvalido' }
  | { tipo: 'PrefijoInvalido' }
  | { tipo: 'PesoCero' }

export function decodificar(codigo: string, config: ConfigEtiqueta): ResultadoDecodificacion {
  if (!validarDigitoVerificador(codigo)) {
    return { tipo: 'DigitoVerificadorInvalido' }
  }

  const prefijo = Number(codigo.substring(0, 2))
  if (prefijo < config.prefijoDesde || prefijo > config.prefijoHasta) {
    return { tipo: 'PrefijoInvalido' }
  }

  const plu = Number(codigo.substring(config.inicioPlu, config.inicioPlu + config.largoPlu))
  const valorCrudo = Number(codigo.substring(config.inicioValor, config.inicioValor + config.largoValor))

  // tipoValor == importe: mismo criterio que el backend (sin precio por kg configurado no
  // hay forma de recuperar el peso real) — se trata igual que "peso" a falta de una
  // definición mejor.
  const kg = valorCrudo / 10 ** config.decimales

  if (kg <= 0) {
    return { tipo: 'PesoCero' }
  }
  return { tipo: 'Exito', plu, kg }
}
