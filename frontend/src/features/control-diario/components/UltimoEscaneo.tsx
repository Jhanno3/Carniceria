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
  SIN_CONEXION_SIN_CACHE: 'Sin conexión y sin datos para trabajar offline — conectate al menos una vez.',
}

export function UltimoEscaneo({ resultado }: UltimoEscaneoProps) {
  if (!resultado) {
    return <p className="text-cuerpo text-texto-secundario">Todavía no escaneaste nada.</p>
  }

  if (resultado.tipo === 'exito') {
    return (
      <div className="rounded-xl bg-exito-fondo p-3 text-cuerpo text-exito-texto">
        <p className="font-medium">{resultado.pendiente ? 'Pendiente de sincronizar' : 'Descontado del stock'}</p>
        <p>
          {resultado.corteNombre ?? 'Corte desconocido'} · {formatearKg(resultado.kg)}
        </p>
      </div>
    )
  }

  return (
    <div className="rounded-xl bg-error-fondo p-3 text-cuerpo text-error">
      {MENSAJES_DE_ERROR[resultado.codigo] ?? resultado.mensaje}
    </div>
  )
}
