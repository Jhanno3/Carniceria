import { useQuery } from '@tanstack/react-query'
import { apiFetch } from './apiFetch'
import type { ConfigEtiquetaDto } from './types'

// Compartido entre `ajustes` (la edita) y `control-diario` (plan-fase3.md, Bloque 5: el
// decodificador offline la necesita cacheada, de ahí la política
// config_etiqueta_empleado_select de V19). La mutación (PUT) sigue siendo solo de
// `ajustes` — ver features/ajustes/api/useActualizarConfigEtiqueta.ts.
export function useConfigEtiqueta() {
  return useQuery({
    queryKey: ['config-etiqueta'],
    queryFn: () => apiFetch<ConfigEtiquetaDto>('/config-etiqueta'),
  })
}
