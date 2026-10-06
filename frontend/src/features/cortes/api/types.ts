// Refleja CorteResponse ya extendido con precioVenta (Fase 5, FR-501).
export type { CorteResponse as Corte } from '../../../shared/api/types'

/**
 * Valores tal cual los tipea FormularioCorte — PLU y precioVenta como texto (es-AR,
 * sin normalizar), igual criterio que DespostadoPage con pesoKg/precioKg: el componente
 * no toca el formato, eso lo hace el hook (useCrearCorte/useActualizarCorte).
 */
export interface CorteFormValues {
  nombre: string
  plu: string
  cuarto: 'Delantero' | 'Trasero' | 'Ambos'
  zonaMapa: string
  activo: boolean
  precioVenta: string
}

/** Body real de `POST`/`PUT /cortes` (contracts/despostado-api.md + FR-501). */
export interface CorteRequest {
  nombre: string
  plu: number
  cuarto: 'Delantero' | 'Trasero' | 'Ambos'
  zonaMapa: string | null
  activo: boolean
  precioVenta: string | null
}
