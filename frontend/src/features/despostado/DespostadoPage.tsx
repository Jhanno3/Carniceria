import { useMemo, useState } from 'react'
import { useCortes } from './api/useCortes'
import { useEstimacion } from './api/useEstimacion'
import { useCargarEntrada } from './api/useCargarEntrada'
import { BarraComposicion } from './components/BarraComposicion'
import { MapaCortes } from './components/MapaCortes'
import { SelectorModoCarga } from './components/SelectorModoCarga'
import { TablaCortes } from './components/TablaCortes'
import { TarjetasPerdida } from './components/TarjetasPerdida'
import { TarjetasResumen } from './components/TarjetasResumen'
import { calcularResumen } from './modelo/resumen'
import { parsearNumero } from '../../shared/formato/formatoEsAr'
import { Modal } from '../../shared/ui/Modal'
import type { CategoriaAnimal } from './api/types'

// Clasificación típica de Mercado de Liniers (V17__categoria_animal.sql).
const CATEGORIAS_ANIMAL: CategoriaAnimal[] = ['Novillo', 'Novillito', 'Vaquillona', 'Vaca', 'Toro', 'Ternero']

// especificacion-carniceria.md, sección 2.2 — PLU provisorio asignado en V3__seed_cortes.sql,
// cubre los 17 cortes originales nada más (reproduce el ejemplo numérico obligatorio de
// la especificación: 100 kg → 81 kg vendible). Los 7 cortes nuevos de V21__cortes_nuevos.sql
// (PLU 16/18/19/20/22/23/24 — los 4 primeros reusan los que dejaron libres Aguja/Marucha/
// Pecho/Cogote al sacarse en V8) no tienen kg de ejemplo acá: "Restablecer ejemplo" los
// deja en blanco, no rompe el total del ejemplo original.
const KG_DE_EJEMPLO_POR_PLU: Record<number, number> = {
  1: 3.0, 2: 1.8, 3: 4.5, 4: 6.0, 5: 2.5, 6: 1.2, 7: 3.0, 8: 1.4,
  12: 3.3, 9: 1.3, 10: 3.5, 11: 11.0, 13: 1.5, 14: 4.0, 15: 4.0,
  17: 6.5, 21: 6.5,
}

function aNumeroSeguro(texto: string): number {
  try {
    return parsearNumero(texto)
  } catch {
    return 0
  }
}

function aTextoPlano(texto: string): string {
  return String(aNumeroSeguro(texto))
}

interface DespostadoPageProps {
  onIrAInicio: () => void
}

export function DespostadoPage({ onIrAInicio }: DespostadoPageProps) {
  const { data: cortes } = useCortes()
  const [proveedor, setProveedor] = useState('')
  const [pesoKgTexto, setPesoKgTexto] = useState('')
  const [precioKgTexto, setPrecioKgTexto] = useState('')
  const [categoria, setCategoria] = useState<CategoriaAnimal | ''>('')
  const [modo, setModo] = useState<'manual' | 'automatico' | null>(null)
  const [kgPorCorte, setKgPorCorte] = useState<Record<string, string>>({})
  const [perdidas, setPerdidas] = useState({ hueso: '', grasa: '', merma: '' })
  const [corteSeleccionadoId, setCorteSeleccionadoId] = useState<string | null>(null)

  const pesoKg = aNumeroSeguro(pesoKgTexto)
  const estimacion = useEstimacion(pesoKg > 0 ? pesoKg : null)
  const cargarEntrada = useCargarEntrada()

  const kgPorCorteNumerico = useMemo(() => {
    const mapa: Record<string, number> = {}
    for (const [corteId, texto] of Object.entries(kgPorCorte)) {
      if (texto.trim() !== '') mapa[corteId] = aNumeroSeguro(texto)
    }
    return mapa
  }, [kgPorCorte])

  const resumen = useMemo(
    () =>
      calcularResumen({
        pesoKg: pesoKg > 0 ? pesoKg : 1, // evita que la vista reviente mientras no hay peso; 1 kg es un valor transitorio.
        precioKg: precioKgTexto.trim() === '' ? null : aNumeroSeguro(precioKgTexto),
        kgPorCorte: kgPorCorteNumerico,
        perdidas: {
          hueso: aNumeroSeguro(perdidas.hueso),
          grasa: aNumeroSeguro(perdidas.grasa),
          merma: aNumeroSeguro(perdidas.merma),
        },
      }),
    [pesoKg, precioKgTexto, kgPorCorteNumerico, perdidas],
  )

  const zonas = useMemo(() => {
    if (!cortes) return []
    const kgPorZona = new Map<string, number>()
    for (const corte of cortes) {
      if (!corte.zonaMapa) continue
      const kg = kgPorCorteNumerico[corte.id] ?? 0
      kgPorZona.set(corte.zonaMapa, (kgPorZona.get(corte.zonaMapa) ?? 0) + kg)
    }
    return Array.from(kgPorZona.entries()).map(([id, kg]) => ({ id, kg }))
  }, [cortes, kgPorCorteNumerico])

  const corteSeleccionado = cortes?.find((c) => c.id === corteSeleccionadoId) ?? null
  const detalle = corteSeleccionado
    ? {
        nombre: corteSeleccionado.nombre,
        kg: kgPorCorteNumerico[corteSeleccionado.id] ?? 0,
        porcentajeDeLaMediaRes: pesoKg > 0 ? ((kgPorCorteNumerico[corteSeleccionado.id] ?? 0) / pesoKg) * 100 : 0,
      }
    : null

  function elegirModo(modoElegido: 'manual' | 'automatico') {
    if (modoElegido === 'automatico' && estimacion.data) {
      const prefill: Record<string, string> = {}
      for (const corte of estimacion.data.cortes) {
        prefill[corte.corteId] = corte.kgEstimado
      }
      setKgPorCorte(prefill)
    }
    setModo(modoElegido)
  }

  function restablecerEjemplo() {
    if (!cortes) return
    const prefill: Record<string, string> = {}
    for (const corte of cortes) {
      const kgEjemplo = KG_DE_EJEMPLO_POR_PLU[corte.plu]
      if (kgEjemplo != null) prefill[corte.id] = String(kgEjemplo)
    }
    setKgPorCorte(prefill)
    setPerdidas({ hueso: '11', grasa: '6', merma: '2' })
    setPesoKgTexto('100')
    setPrecioKgTexto('5200')
    setCategoria('Novillo')
    setModo('manual')
  }

  function cargarLaEntrada() {
    cargarEntrada.mutate({
      proveedor: proveedor.trim() === '' ? null : proveedor,
      pesoKg: aTextoPlano(pesoKgTexto),
      precioKg: precioKgTexto.trim() === '' ? null : aTextoPlano(precioKgTexto),
      categoria: categoria === '' ? null : categoria,
      cortes: Object.entries(kgPorCorte)
        .filter(([, texto]) => texto.trim() !== '')
        .map(([corteId, texto]) => ({ corteId, kg: aTextoPlano(texto) })),
      perdidas: {
        hueso: perdidas.hueso.trim() === '' ? null : aTextoPlano(perdidas.hueso),
        grasa: perdidas.grasa.trim() === '' ? null : aTextoPlano(perdidas.grasa),
        merma: perdidas.merma.trim() === '' ? null : aTextoPlano(perdidas.merma),
      },
    })
  }

  return (
    <main className="mx-auto flex max-w-5xl flex-col gap-6 p-4">
      <h1 className="text-titulo-seccion font-titulos font-bold text-texto">Despostado</h1>

      <div className="flex flex-col gap-1">
        <label htmlFor="proveedor" className="text-etiqueta font-medium uppercase text-texto-secundario">
          Proveedor (opcional)
        </label>
        <input
          id="proveedor"
          type="text"
          value={proveedor}
          onChange={(e) => setProveedor(e.target.value)}
          className="h-11 w-full max-w-sm rounded-xl border border-borde-campo px-3 text-cuerpo"
        />
      </div>

      <div className="flex flex-col gap-1">
        <label htmlFor="categoria" className="text-etiqueta font-medium uppercase text-texto-secundario">
          Categoría del animal (opcional)
        </label>
        <select
          id="categoria"
          value={categoria}
          onChange={(e) => setCategoria(e.target.value as CategoriaAnimal | '')}
          className="h-11 w-full max-w-sm rounded-xl border border-borde-campo px-3 text-cuerpo"
        >
          <option value="">Sin especificar</option>
          {CATEGORIAS_ANIMAL.map((opcion) => (
            <option key={opcion} value={opcion}>
              {opcion}
            </option>
          ))}
        </select>
      </div>

      <TarjetasResumen
        pesoKgTexto={pesoKgTexto}
        onCambiarPesoKg={setPesoKgTexto}
        precioKgTexto={precioKgTexto}
        onCambiarPrecioKg={setPrecioKgTexto}
        resumen={resumen}
      />

      {modo === null ? (
        <SelectorModoCarga disponibleAutomatico={estimacion.disponible} onElegir={elegirModo} />
      ) : (
        <>
          <BarraComposicion
            pesoKg={pesoKg}
            vendibleKg={resumen.vendibleKg}
            hueso={aNumeroSeguro(perdidas.hueso)}
            grasa={aNumeroSeguro(perdidas.grasa)}
            merma={aNumeroSeguro(perdidas.merma)}
            sinAsignarKg={resumen.sinAsignarKg}
          />

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2 lg:items-start">
            <div className="flex flex-col gap-2 rounded-xl border border-borde p-4">
              <h2 className="text-titulo-seccion font-titulos font-bold text-texto">Mapa de cortes</h2>
              <MapaCortes
                zonas={zonas}
                zonaResaltada={corteSeleccionado?.zonaMapa ?? null}
                detalle={detalle}
              />
            </div>

            <div className="flex flex-col rounded-xl border border-borde">
              <div className="flex items-baseline justify-between gap-2 border-b border-borde p-4">
                <h2 className="text-titulo-seccion font-titulos font-bold text-texto">Cortes vendibles</h2>
                <p className="text-etiqueta text-texto-secundario">Valores de ejemplo reemplazados por tus pesadas</p>
              </div>
              {/* Scroll propio (max-h + overflow-y-auto), independiente del scroll de la página. */}
              <div className="max-h-[32rem] overflow-y-auto">
                <TablaCortes
                  cortes={cortes ?? []}
                  kgPorCorte={kgPorCorte}
                  onCambiarKg={(corteId, texto) => setKgPorCorte((previo) => ({ ...previo, [corteId]: texto }))}
                  corteSeleccionadoId={corteSeleccionadoId}
                  onSeleccionarCorte={setCorteSeleccionadoId}
                />
              </div>
            </div>
          </div>

          <TarjetasPerdida
            pesoKg={pesoKg}
            valores={perdidas}
            onCambiar={(tipo, texto) => setPerdidas((previo) => ({ ...previo, [tipo]: texto }))}
          />

          <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
            <button
              type="button"
              onClick={restablecerEjemplo}
              className="h-11 rounded-xl border border-borde-campo px-4 font-medium text-texto"
            >
              Restablecer ejemplo
            </button>
            <button
              type="button"
              disabled={cargarEntrada.isPending}
              onClick={cargarLaEntrada}
              className="h-11 rounded-xl bg-vendible px-4 font-medium text-white disabled:opacity-60"
            >
              {cargarEntrada.isPending ? 'Cargando…' : 'Cargar entrada'}
            </button>
          </div>

          {cargarEntrada.isError && (
            <p className="text-cuerpo text-error">
              No se pudo cargar la entrada: {cargarEntrada.error instanceof Error ? cargarEntrada.error.message : ''}
            </p>
          )}
        </>
      )}

      {cargarEntrada.isSuccess && (
        <Modal titulo="Entrada cargada con éxito" onCerrar={() => cargarEntrada.reset()}>
          <div className="flex flex-col gap-3 sm:flex-row sm:justify-end">
            <button
              type="button"
              onClick={() => cargarEntrada.reset()}
              className="h-11 rounded-xl border border-borde-campo px-4 font-medium text-texto"
            >
              OK
            </button>
            <button
              type="button"
              onClick={() => {
                cargarEntrada.reset()
                onIrAInicio()
              }}
              className="h-11 rounded-xl bg-vendible px-4 font-medium text-white"
            >
              Inicio
            </button>
          </div>
        </Modal>
      )}
    </main>
  )
}
