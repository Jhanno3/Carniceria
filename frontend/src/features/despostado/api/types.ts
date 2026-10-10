// Tipos que reflejan los DTO de contracts/despostado-api.md. Kilos e importes son
// `string` (nunca `number`), igual que en el backend (research.md, "Jackson 3 en
// Spring Boot 4") — se parsean con modelo/resumen o shared/formato según haga falta.

import type { TipoEntrada } from '../modelo/tiposDeEntrada'

// Movido a shared/api/types.ts (plan-fase3.md, Bloque 5): lo necesita también
// `control-diario`. Re-exportado acá para no tocar cada punto de esta feature que ya
// importaba `CorteResponse` desde './types'.
export type { CorteResponse } from '../../../shared/api/types'

export interface CorteKgDto {
  corteId: string
  kg: string
}

export interface PerdidasDto {
  hueso: string | null
  grasa: string | null
  merma: string | null
}

export interface ResumenDto {
  vendibleKg: string
  perdidaKg: string
  sinAsignarKg: string
  rendimientoPorc: string
  costoTotal: string | null
  costoKgVendible: string | null
}

// Clasificación típica de Mercado de Liniers (V17__categoria_animal.sql) — opcional,
// para el reporte "por categoría de animal" de Fase 4.
export type CategoriaAnimal = 'Novillo' | 'Novillito' | 'Vaquillona' | 'Vaca' | 'Toro' | 'Ternero'

// Fase 6, FR-601 — ver modelo/tiposDeEntrada.ts para el mapeo a cortes habilitados.
export type { TipoEntrada }

export interface MediaResResponse {
  id: string
  fecha: string
  proveedor: string | null
  pesoKg: string
  precioKg: string | null
  categoria: CategoriaAnimal | null
  /** Siempre presente (nunca null) — "MediaRes" si no se eligió ninguno en particular. */
  tipoEntrada: TipoEntrada
  despostado: CorteKgDto[]
  perdidas: PerdidasDto
  resumen: ResumenDto
}

export interface CargarEntradaRequest {
  proveedor?: string | null
  pesoKg: string
  precioKg?: string | null
  categoria?: CategoriaAnimal | null
  tipoEntrada?: TipoEntrada | null
  cortes: CorteKgDto[]
  perdidas?: PerdidasDto | null
}

export interface CorteKgEstimadoDto {
  corteId: string
  kgEstimado: string
}

export interface EstimacionResponse {
  cortes: CorteKgEstimadoDto[]
  perdidas: PerdidasDto
}
