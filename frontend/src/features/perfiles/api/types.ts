export interface PerfilResponse {
  id: string
  nombre: string | null
  rol: 'dueno' | 'empleado'
  estado: 'pendiente' | 'aprobado' | 'rechazado'
}
