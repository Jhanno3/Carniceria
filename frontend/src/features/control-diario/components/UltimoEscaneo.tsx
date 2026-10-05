import { formatearKg } from '../../../shared/formato/formatoEsAr'
import type { ResultadoEscaneo } from './CampoEscaneo'

interface UltimoEscaneoProps {
  resultado: ResultadoEscaneo | null
}

const MENSAJES_DE_ERROR: Record<string, string> = {
  DIGITO_VERIFICADOR_INVALIDO: 'El código escaneado no es válido.',
  PREFIJO_INVALIDO: 'El código no corresponde a una etiqueta de peso variable.',
  PLU_INEXISTENTE: 'El código no corresponde a ningún corte activo.',
  PESO_CERO: 'El peso leído es cero.',
}

export function UltimoEscaneo({ resultado }: UltimoEscaneoProps) {
  if (!resultado) {
    return <p className="text-cuerpo text-texto-secundario">Todavía no escaneaste nada.</p>
  }

  if (resultado.tipo === 'exito') {
    return (
      <div className="rounded-xl bg-exito-fondo p-3 text-cuerpo text-exito-texto">
        <p className="font-medium">Descontado del stock</p>
        <p>
          {resultado.venta.corteNombre ?? 'Corte desconocido'} · {formatearKg(Number(resultado.venta.kg))}
        </p>
      </div>
    )
  }

  return (
    <div className="rounded-xl bg-red-50 p-3 text-cuerpo text-error">
      {MENSAJES_DE_ERROR[resultado.codigo] ?? resultado.mensaje}
    </div>
  )
}
