import { useQuery } from '@tanstack/react-query'
import { apiFetch } from '../../../shared/api/apiFetch'
import type { MediaResResponse } from './types'

/** GET /medias-reses?desde=&hasta= — fechas en formato ISO (yyyy-MM-dd). */
export function useMediasReses(desde: string, hasta: string) {
  return useQuery({
    queryKey: ['medias-reses', desde, hasta],
    queryFn: () => apiFetch<MediaResResponse[]>(`/medias-reses?desde=${desde}&hasta=${hasta}`),
  })
}
