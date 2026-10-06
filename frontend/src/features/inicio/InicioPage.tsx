import { useMediasReses } from '../despostado/api/useMediasReses'
import { useVentas } from '../control-diario/api/useVentas'
import { calcularResumenMes } from './modelo/resumenMes'
import { formatearKg, formatearPesos } from '../../shared/formato/formatoEsAr'
import { usePerfilPropio } from '../perfiles/api/usePerfilPropio'
import { InvitarEmpleado } from '../perfiles/components/InvitarEmpleado'

function formatearPorcentaje(valor: number): string {
  return `${new Intl.NumberFormat('es-AR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(valor)} %`
}

function inicioYFinDeEsteMes(): { desde: string; hasta: string } {
  const hoy = new Date()
  const desde = new Date(hoy.getFullYear(), hoy.getMonth(), 1)
  const aIso = (fecha: Date) => fecha.toISOString().slice(0, 10)
  return { desde: aIso(desde), hasta: aIso(hoy) }
}

function Tarjeta({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-2 rounded-xl border border-borde p-4">
      <h3 className="text-etiqueta font-medium uppercase text-texto-secundario">{titulo}</h3>
      {children}
    </div>
  )
}

export function InicioPage() {
  const { desde, hasta } = inicioYFinDeEsteMes()
  const { data: entradas, isLoading, isError } = useMediasReses(desde, hasta)
  const { data: ventas } = useVentas(desde, hasta)
  const { data: perfil } = usePerfilPropio(true)

  const nombreMes = new Intl.DateTimeFormat('es-AR', { month: 'long', year: 'numeric' }).format(new Date())

  return (
    <main className="flex flex-col gap-6 p-4">
      <h1 className="text-titulo-seccion font-titulos font-bold text-texto">Resumen de {nombreMes}</h1>

      {isError && <p className="text-cuerpo text-error">No pudimos cargar el resumen del mes.</p>}

      {isLoading && !isError && <p className="text-cuerpo text-texto-secundario">Cargando…</p>}

      {entradas && (() => {
        const resumen = calcularResumenMes(entradas, ventas ?? [])
        return (
          <>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <Tarjeta titulo="Medias reses cargadas">
                <p className="numero text-cifra-tarjeta font-titulos font-bold text-texto">
                  {resumen.cantidadEntradas}
                </p>
              </Tarjeta>

              <Tarjeta titulo="Kilos vendibles del mes">
                <p className="numero text-cifra-tarjeta font-titulos font-bold text-vendible">
                  {formatearKg(resumen.vendibleKgTotal)}
                </p>
              </Tarjeta>

              <Tarjeta titulo="Rendimiento promedio">
                {resumen.rendimientoPromedioPorc != null ? (
                  <p className="numero text-cifra-tarjeta font-titulos font-bold text-texto">
                    {formatearPorcentaje(resumen.rendimientoPromedioPorc)}
                  </p>
                ) : (
                  <p className="text-cifra-tarjeta font-titulos font-bold text-texto-secundario">—</p>
                )}
              </Tarjeta>

              <Tarjeta titulo="Costo promedio por kg vendible">
                {resumen.costoKgVendiblePromedio != null ? (
                  <p className="numero text-cifra-tarjeta font-titulos font-bold text-texto">
                    {formatearPesos(resumen.costoKgVendiblePromedio)}
                  </p>
                ) : (
                  <>
                    <p className="text-cifra-tarjeta font-titulos font-bold text-texto-secundario">—</p>
                    <p className="text-cuerpo text-texto-secundario">Ninguna entrada del mes tiene precio cargado</p>
                  </>
                )}
              </Tarjeta>

              <Tarjeta titulo="Recaudado este mes">
                <p className="numero text-cifra-tarjeta font-titulos font-bold text-vendible">
                  {formatearPesos(resumen.dineroRecaudadoMes)}
                </p>
              </Tarjeta>
            </div>

            {resumen.ventasSinPrecioMes > 0 && (
              <p className="text-cuerpo text-texto-secundario">
                {resumen.ventasSinPrecioMes} ventas de este mes sin precio registrado.
              </p>
            )}
          </>
        )
      })()}

      {entradas && entradas.length === 0 && (
        <p className="text-cuerpo text-texto-secundario">Todavía no cargaste ninguna media res este mes.</p>
      )}

      {/* "Dueño de sí mismo" (duenoId === su propio id), no "rol === dueno" a secas: un
          admin que también opera su propio negocio de prueba puede invitar empleados igual. */}
      {perfil?.duenoId === perfil?.id && perfil && <InvitarEmpleado duenoId={perfil.id} />}
    </main>
  )
}
