import { useState } from 'react'
import { SelectorDeRango } from './components/SelectorDeRango'
import { TablaReporte } from './components/TablaReporte'
import type { FilaReporte } from './components/TablaReporte'
import { useReportePorProveedor } from './api/useReportePorProveedor'
import { useReportePorCategoria } from './api/useReportePorCategoria'
import { useReportePorPeriodo } from './api/useReportePorPeriodo'
import type { Periodo } from './api/types'

const DIA_MS = 24 * 60 * 60 * 1000

function hoyIso(): string {
  return new Date().toISOString().slice(0, 10)
}

function haceNDiasIso(n: number): string {
  return new Date(Date.now() - n * DIA_MS).toISOString().slice(0, 10)
}

function rangoPorDefecto() {
  return { desde: haceNDiasIso(29), hasta: hoyIso() }
}

/** US-4.1 (FR-401/FR-402/FR-403): rendimiento por proveedor, categoría y período. Solo dueño/admin-con-negocio. */
export function ReportesPage() {
  const [rangoProveedor, setRangoProveedor] = useState(rangoPorDefecto)
  const [rangoCategoria, setRangoCategoria] = useState(rangoPorDefecto)
  const [rangoPeriodo, setRangoPeriodo] = useState(rangoPorDefecto)
  const [periodo, setPeriodo] = useState<Periodo>('semana')

  const porProveedor = useReportePorProveedor(rangoProveedor.desde, rangoProveedor.hasta)
  const porCategoria = useReportePorCategoria(rangoCategoria.desde, rangoCategoria.hasta)
  const porPeriodo = useReportePorPeriodo(rangoPeriodo.desde, rangoPeriodo.hasta, periodo)

  const filasProveedor: FilaReporte[] = (porProveedor.data ?? []).map((item) => ({
    grupo: item.proveedor,
    cantidadEntradas: item.cantidadEntradas,
    rendimientoPromedioPorc: item.rendimientoPromedioPorc,
    costoKgVendiblePromedio: item.costoKgVendiblePromedio,
    beneficioPorKgVendible: item.beneficioPorKgVendible,
  }))

  const filasCategoria: FilaReporte[] = (porCategoria.data ?? []).map((item) => ({
    grupo: item.categoria,
    cantidadEntradas: item.cantidadEntradas,
    rendimientoPromedioPorc: item.rendimientoPromedioPorc,
    costoKgVendiblePromedio: item.costoKgVendiblePromedio,
    beneficioPorKgVendible: item.beneficioPorKgVendible,
  }))

  const filasPeriodo: FilaReporte[] = (porPeriodo.data ?? []).map((item) => ({
    grupo: new Date(`${item.periodoInicio}T00:00:00`).toLocaleDateString('es-AR'),
    cantidadEntradas: item.cantidadEntradas,
    rendimientoPromedioPorc: item.rendimientoPromedioPorc,
    costoKgVendiblePromedio: item.costoKgVendiblePromedio,
    beneficioPorKgVendible: item.beneficioPorKgVendible,
  }))

  return (
    <main className="mx-auto flex max-w-5xl flex-col gap-8 p-4">
      <h1 className="text-titulo-seccion font-titulos font-bold text-texto">Reportes</h1>

      <section className="flex flex-col gap-3">
        <h2 className="text-titulo-seccion font-titulos font-bold text-texto">Por proveedor</h2>
        <SelectorDeRango
          idPrefix="reporte-proveedor"
          desde={rangoProveedor.desde}
          hasta={rangoProveedor.hasta}
          onAplicar={(desde, hasta) => setRangoProveedor({ desde, hasta })}
        />
        <TablaReporte etiquetaGrupo="Proveedor" etiquetaSinValor="Sin proveedor" filas={filasProveedor} />
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-titulo-seccion font-titulos font-bold text-texto">Por categoría</h2>
        <SelectorDeRango
          idPrefix="reporte-categoria"
          desde={rangoCategoria.desde}
          hasta={rangoCategoria.hasta}
          onAplicar={(desde, hasta) => setRangoCategoria({ desde, hasta })}
        />
        <TablaReporte etiquetaGrupo="Categoría" etiquetaSinValor="Sin categoría" filas={filasCategoria} />
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-titulo-seccion font-titulos font-bold text-texto">Por período</h2>
        <div className="flex flex-wrap items-end gap-3">
          <SelectorDeRango
            idPrefix="reporte-periodo"
            desde={rangoPeriodo.desde}
            hasta={rangoPeriodo.hasta}
            onAplicar={(desde, hasta) => setRangoPeriodo({ desde, hasta })}
          />
          <div className="flex flex-col gap-1">
            <label
              htmlFor="reporte-periodo-granularidad"
              className="text-etiqueta font-medium uppercase text-texto-secundario"
            >
              Agrupar por
            </label>
            <select
              id="reporte-periodo-granularidad"
              value={periodo}
              onChange={(e) => setPeriodo(e.target.value as Periodo)}
              className="h-11 rounded-xl border border-borde-campo px-3 text-cuerpo"
            >
              <option value="dia">Día</option>
              <option value="semana">Semana</option>
              <option value="mes">Mes</option>
            </select>
          </div>
        </div>
        <TablaReporte etiquetaGrupo="Período" etiquetaSinValor="—" filas={filasPeriodo} />
      </section>
    </main>
  )
}
