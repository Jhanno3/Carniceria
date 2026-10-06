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
import { calcularGananciaEstimada } from './modelo/gananciaEstimada'
import { TIPOS_DE_ENTRADA, cortesHabilitados } from './modelo/tiposDeEntrada'
import { parsearNumero } from '../../shared/formato/formatoEsAr'
import { Modal } from '../../shared/ui/Modal'
import type { CategoriaAnimal, TipoEntrada } from './api/types'

// Clasificación típica de Mercado de Liniers (V17__categoria_animal.sql).
const CATEGORIAS_ANIMAL: CategoriaAnimal[] = ['Novillo', 'Novillito', 'Vaquillona', 'Vaca', 'Toro', 'Ternero']

// Ejemplo de referencia real (dueño, 2026-10) para una media res de 100 kg, cubriendo los
// 28 cortes del catálogo completo (17 originales + 7 de V21__cortes_nuevos.sql + 4
// reactivados por V22__reactivar_cortes_redundantes.sql) — a diferencia del "ejemplo
// numérico obligatorio" de especificacion-carniceria.md sección 6 (100 kg → 81 kg
// vendible, usado como fixture propio en los tests automatizados, no tocado acá), este da
// 82,8 kg vendibles / 17,2 kg de pérdida. Es solo el prefill de "Restablecer ejemplo", no
// afecta ningún test.
const KG_DE_EJEMPLO_POR_PLU: Record<number, number> = {
  1: 3.0, 2: 1.8, 3: 4.5, 4: 6.5, 5: 3.5, 6: 2.0, 7: 3.5, 8: 1.6,
  9: 1.8, 10: 3.0, 11: 7.0, 12: 3.5, 13: 1.5, 14: 3.0, 15: 4.0,
  16: 2.5, 17: 3.5, 18: 2.5, 19: 0.8, 20: 1.0, 21: 6.0, 22: 1.0,
  23: 0.6, 24: 1.5, 25: 5.0, 26: 1.2, 27: 4.0, 28: 3.0,
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
  const [tipoEntrada, setTipoEntrada] = useState<TipoEntrada>('MediaRes')
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

  const precioVentaPorCorte = useMemo(() => {
    const mapa: Record<string, number | null> = {}
    for (const corte of cortes ?? []) {
      mapa[corte.id] = corte.precioVenta == null ? null : Number(corte.precioVenta)
    }
    return mapa
  }, [cortes])

  const gananciaEstimada = useMemo(
    () => calcularGananciaEstimada(kgPorCorteNumerico, resumen.costoKgVendible, precioVentaPorCorte),
    [kgPorCorteNumerico, resumen.costoKgVendible, precioVentaPorCorte],
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
    setPerdidas({ hueso: '8.5', grasa: '6', merma: '2.7' })
    setPesoKgTexto('100')
    setPrecioKgTexto('5200')
    setCategoria('Novillo')
    // El ejemplo canónico reparte kilos en cortes de varios tipos a la vez (lomo, cuadril,
    // asado, etc.), así que solo tiene sentido mostrado sin ninguna atenuación.
    setTipoEntrada('MediaRes')
    setModo('manual')
  }

  function cargarLaEntrada() {
    cargarEntrada.mutate({
      proveedor: proveedor.trim() === '' ? null : proveedor,
      pesoKg: aTextoPlano(pesoKgTexto),
      precioKg: precioKgTexto.trim() === '' ? null : aTextoPlano(precioKgTexto),
      categoria: categoria === '' ? null : categoria,
      tipoEntrada,
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

      <div className="flex flex-wrap gap-4">
        <div className="flex flex-1 flex-col gap-1">
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

        <div className="flex flex-1 flex-col gap-1">
          <label htmlFor="tipo-entrada" className="text-etiqueta font-medium uppercase text-texto-secundario">
            Corte
          </label>
          <select
            id="tipo-entrada"
            value={tipoEntrada}
            onChange={(e) => setTipoEntrada(e.target.value as TipoEntrada)}
            className="h-11 w-full max-w-sm rounded-xl border border-borde-campo px-3 text-cuerpo"
          >
            {TIPOS_DE_ENTRADA.map(({ valor, etiqueta }) => (
              <option key={valor} value={valor}>
                {etiqueta}
              </option>
            ))}
          </select>
        </div>
      </div>

      <TarjetasResumen
        pesoKgTexto={pesoKgTexto}
        onCambiarPesoKg={setPesoKgTexto}
        precioKgTexto={precioKgTexto}
        onCambiarPrecioKg={setPrecioKgTexto}
        resumen={resumen}
        gananciaEstimada={gananciaEstimada}
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
                  corteNombresHabilitados={cortesHabilitados(tipoEntrada)}
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
