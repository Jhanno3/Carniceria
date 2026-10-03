import { supabase } from '../supabase/cliente'

/** Mismo formato que shared/error/ErrorResponse.java del backend: { error, mensaje }. */
export class ApiError extends Error {
  readonly codigo: string
  readonly status: number

  constructor(codigo: string, mensaje: string, status: number) {
    super(mensaje)
    this.codigo = codigo
    this.status = status
  }
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL

/** Pega contra la API propia (nunca contra Supabase directo, salvo el login). */
export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const { data } = await supabase.auth.getSession()
  const token = data.session?.access_token

  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  })

  if (!response.ok) {
    const cuerpo = await response.json().catch(() => null)
    throw new ApiError(
      cuerpo?.error ?? 'ERROR_DESCONOCIDO',
      cuerpo?.mensaje ?? 'Ocurrió un error inesperado.',
      response.status,
    )
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}
