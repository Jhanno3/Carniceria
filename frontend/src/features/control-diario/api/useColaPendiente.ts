import { useQuery } from '@tanstack/react-query'
import { contar } from '../cola/colaVentas'

/** FR-303: cuántas ventas están pendientes de subir. Cola local, consulta barata. */
export function useColaPendiente() {
  return useQuery({
    queryKey: ['cola-pendiente'],
    queryFn: contar,
    refetchInterval: 2000,
  })
}
