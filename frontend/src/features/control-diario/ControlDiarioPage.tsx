import { useState } from 'react'
import { CampoEscaneo } from './components/CampoEscaneo'
import type { ResultadoEscaneo } from './components/CampoEscaneo'
import { UltimoEscaneo } from './components/UltimoEscaneo'
import { ResumenDia } from './components/ResumenDia'
import { VentasDeHoy } from './components/VentasDeHoy'
import { TablaStock } from './components/TablaStock'
import { IndicadorPendientes } from './components/IndicadorPendientes'
import { useVentas } from './api/useVentas'
import { useStock } from './api/useStock'
import { useResumenDia } from './api/useResumenDia'
import { useSincronizarCola } from './api/useSincronizarCola'
import { usePerfilPropio } from '../perfiles/api/usePerfilPropio'

function hoyIso(): string {
  return new Date().toISOString().slice(0, 10)
}

export function ControlDiarioPage() {
  const [ultimoResultado, setUltimoResultado] = useState<ResultadoEscaneo | null>(null)
  const hoy = hoyIso()
  const { data: ventas } = useVentas(hoy, hoy)
  const { data: stock } = useStock()
  const { data: resumen } = useResumenDia()
  const { data: perfil } = usePerfilPropio(true)
  const { noSincronizadas } = useSincronizarCola()

  return (
    <main className="mx-auto flex max-w-5xl flex-col gap-6 p-4">
      <h1 className="text-titulo-seccion font-titulos font-bold text-texto">Control diario</h1>

      <IndicadorPendientes />

      {noSincronizadas.length > 0 && (
        <div className="rounded-xl bg-error-fondo p-3 text-cuerpo text-error">
          <p className="font-medium">No se pudieron sincronizar {noSincronizadas.length} venta(s):</p>
          <ul className="list-inside list-disc">
            {noSincronizadas.map((v, i) => (
              <li key={i}>
                {v.codigo} — {v.mensaje}
              </li>
            ))}
          </ul>
        </div>
      )}

      <CampoEscaneo onResultado={setUltimoResultado} />
      <UltimoEscaneo resultado={ultimoResultado} />

      {resumen && <ResumenDia resumen={resumen} />}

      {perfil && (
        <VentasDeHoy
          ventas={ventas ?? []}
          usuarioActualId={perfil.id}
          puedeAnularCualquiera={perfil.rol === 'dueno' || perfil.rol === 'admin'}
        />
      )}

      <div className="flex flex-col gap-2">
        <h2 className="text-titulo-seccion font-titulos font-bold text-texto">Stock por corte</h2>
        <TablaStock stock={stock ?? []} />
      </div>
    </main>
  )
}
