export interface PerfilResponse {
  id: string
  nombre: string | null
  rol: 'admin' | 'dueno' | 'empleado'
  estado: 'pendiente' | 'aprobado' | 'rechazado'
  duenoId: string | null
}
