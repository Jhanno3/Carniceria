import { parsearNumero } from '../../../shared/formato/formatoEsAr'
import type { CorteFormValues, CorteRequest } from './types'

/** Normaliza los valores tal cual los tipeó el formulario al body que espera el backend. */
export function aCorteRequest(valores: CorteFormValues): CorteRequest {
  return {
    nombre: valores.nombre,
    plu: parsearNumero(valores.plu),
    // cuarto no aplica fuera de "Vacuno" (Fase 7) — el backend lo rechaza si viene con un
    // valor para esos tipos, así que ni se manda.
    cuarto: valores.tipoProducto === 'Vacuno' ? valores.cuarto : null,
    tipoProducto: valores.tipoProducto,
    zonaMapa: valores.zonaMapa.trim() === '' ? null : valores.zonaMapa,
    activo: valores.activo,
    precioVenta: valores.precioVenta.trim() === '' ? null : String(parsearNumero(valores.precioVenta)),
    descuentaStockDeCorteId: valores.descuentaStockDeCorteId.trim() === '' ? null : valores.descuentaStockDeCorteId,
  }
}
