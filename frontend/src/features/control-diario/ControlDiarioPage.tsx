import { useState } from 'react'
import { CampoEscaneo } from './components/CampoEscaneo'
import type { ResultadoEscaneo } from './components/CampoEscaneo'
import { UltimoEscaneo } from './components/UltimoEscaneo'
import { ResumenDia } from './components/ResumenDia'
import { VentasDeHoy } from './components/VentasDeHoy'
import { TablaStock } from './components/TablaStock'
import { useVentas } from './api/useVentas'
import { useStock } from './api/useStock'
import { useResumenDia } from './api/useResumenDia'

function hoyIso(): string {
  return new Date().toISOString().slice(0, 10)
}

export function ControlDiarioPage() {
  const [ultimoResultado, setUltimoResultado] = useState<ResultadoEscaneo | null>(null)
  const hoy = hoyIso()
  const { data: ventas } = useVentas(hoy, hoy)
  const { data: stock } = useStock()
  const { data: resumen } = useResumenDia()

  return (
    <main className="mx-auto flex max-w-5xl flex-col gap-6 p-4">
      <h1 className="text-titulo-seccion font-titulos font-bold text-texto">Control diario</h1>

      <CampoEscaneo onResultado={setUltimoResultado} />
      <UltimoEscaneo resultado={ultimoResultado} />

      {resumen && <ResumenDia resumen={resumen} />}

      <VentasDeHoy ventas={ventas ?? []} />

      <div className="flex flex-col gap-2">
        <h2 className="text-titulo-seccion font-titulos font-bold text-texto">Stock por corte</h2>
        <TablaStock stock={stock ?? []} />
      </div>
    </main>
  )
}
