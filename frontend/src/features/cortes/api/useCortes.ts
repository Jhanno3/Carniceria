// Esta pantalla necesita ver los inactivos para poder reactivarlos (a diferencia de
// `despostado`/`control-diario`, que solo leen cortes activos) — por eso vuelve a pasar
// por `shared/api/useCortes`, que ya acepta `incluirInactivos`.
export { useCortes } from '../../../shared/api/useCortes'
