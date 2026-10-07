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
  // Solo se usa/envía cuando tipoProducto es "Vacuno" (Fase 7) — el valor queda guardado
  // en el formulario igual si el dueño alterna tipoProducto de ida y vuelta.
  cuarto: 'Delantero' | 'Trasero' | 'Ambos'
  tipoProducto: 'Vacuno' | 'AchurasEmbutidos' | 'Cerdo' | 'Carne'
  zonaMapa: string
  activo: boolean
  precioVenta: string
}

/** Body real de `POST`/`PUT /cortes` (contracts/despostado-api.md + FR-501 + Fase 7). */
export interface CorteRequest {
  nombre: string
  plu: number
  cuarto: 'Delantero' | 'Trasero' | 'Ambos' | null
  tipoProducto: 'Vacuno' | 'AchurasEmbutidos' | 'Cerdo' | 'Carne'
  zonaMapa: string | null
  activo: boolean
  precioVenta: string | null
}
