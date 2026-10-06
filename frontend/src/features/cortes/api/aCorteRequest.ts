import { parsearNumero } from '../../../shared/formato/formatoEsAr'
import type { CorteFormValues, CorteRequest } from './types'

/** Normaliza los valores tal cual los tipeó el formulario al body que espera el backend. */
export function aCorteRequest(valores: CorteFormValues): CorteRequest {
  return {
    nombre: valores.nombre,
    plu: parsearNumero(valores.plu),
    cuarto: valores.cuarto,
    zonaMapa: valores.zonaMapa.trim() === '' ? null : valores.zonaMapa,
    activo: valores.activo,
    precioVenta: valores.precioVenta.trim() === '' ? null : String(parsearNumero(valores.precioVenta)),
  }
}
