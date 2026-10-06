import type { Resumen } from '../modelo/resumen'
import { formatearKg, formatearPesos, parsearNumero } from '../../../shared/formato/formatoEsAr'

function aNumeroSeguro(texto: string, porDefecto = 0): number {
  try {
    return parsearNumero(texto)
  } catch {
    return porDefecto
  }
}

interface TarjetasResumenProps {
  pesoKgTexto: string
  onCambiarPesoKg: (texto: string) => void
  precioKgTexto: string
  onCambiarPrecioKg: (texto: string) => void
  resumen: Resumen
  /** Suma de (precioVenta - costoKgVendible) × kg, por cada corte con ambos cargados. */
  gananciaEstimada: number | null
}

function Tarjeta({ titulo, children }: { titulo: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-2 rounded-xl border border-borde p-4">
      <h3 className="text-etiqueta font-medium uppercase text-texto-secundario">{titulo}</h3>
      {children}
    </div>
  )
}

export function TarjetasResumen({
  pesoKgTexto,
  onCambiarPesoKg,
  precioKgTexto,
  onCambiarPrecioKg,
  resumen,
  gananciaEstimada,
}: TarjetasResumenProps) {
  const tienePrecio = resumen.costoKgVendible != null

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
      <Tarjeta titulo="Entrada">
        <label htmlFor="peso-media-res" className="text-etiqueta text-texto-secundario">
          Peso media res (kg)
        </label>
        <input
          id="peso-media-res"
          type="text"
          inputMode="decimal"
          value={pesoKgTexto}
          onChange={(e) => onCambiarPesoKg(e.target.value)}
          className="numero h-11 rounded-xl border border-borde-campo px-3 text-cifra-tarjeta font-titulos"
        />
        <label htmlFor="precio-compra" className="text-etiqueta text-texto-secundario">
          Precio de compra ($/kg)
        </label>
        <input
          id="precio-compra"
          type="text"
          inputMode="decimal"
          value={precioKgTexto}
          onChange={(e) => onCambiarPrecioKg(e.target.value)}
          className="numero h-11 rounded-xl border border-borde-campo px-3 text-cuerpo"
        />
      </Tarjeta>

      <Tarjeta titulo="Carne vendible">
        <p className="numero text-cifra-tarjeta font-titulos font-bold text-vendible">
          {formatearKg(resumen.vendibleKg)}
        </p>
        <p className="numero text-cuerpo text-texto-secundario">{formatearPorcentaje(resumen.rendimientoPorc)}</p>
      </Tarjeta>

      <Tarjeta titulo="Pérdida">
        <p className="numero text-cifra-tarjeta font-titulos font-bold text-hueso">{formatearKg(resumen.perdidaKg)}</p>
        <p className="numero text-cuerpo text-texto-secundario">
          {formatearPorcentaje((resumen.perdidaKg / aNumeroSeguro(pesoKgTexto, 1)) * 100)}
        </p>
      </Tarjeta>

      <Tarjeta titulo="Costo real por kg vendible">
        {tienePrecio ? (
          <>
            <p className="numero text-cifra-tarjeta font-titulos font-bold text-texto">
              {formatearPesos(resumen.costoKgVendible as number)}
            </p>
            <p className="numero text-cuerpo text-texto-secundario">
              Pagaste {formatearPesos(aNumeroSeguro(precioKgTexto))}/kg de media res
            </p>
          </>
        ) : (
          <>
            <p className="text-cifra-tarjeta font-titulos font-bold text-texto-secundario">—</p>
            <p className="text-cuerpo text-texto-secundario">Cargá el precio de compra</p>
          </>
        )}
      </Tarjeta>

      <Tarjeta titulo="Ganancia estimada">
        {gananciaEstimada != null ? (() => {
          const texto = formatearPesos(gananciaEstimada)
          // Pasadas las 6 cifras, 34px ya no entra en una línea dentro de la tarjeta.
          const esNumeroLargo = texto.replace(/\D/g, '').length > 6
          return (
            <p
              className={`numero whitespace-nowrap font-titulos font-bold ${esNumeroLargo ? 'text-cifra-tarjeta-chica' : 'text-cifra-tarjeta'} ${gananciaEstimada >= 0 ? 'text-exito-texto' : 'text-error'}`}
            >
              {texto}
            </p>
          )
        })() : !tienePrecio ? (
          <>
            <p className="text-cifra-tarjeta font-titulos font-bold text-texto-secundario">—</p>
            <p className="text-cuerpo text-texto-secundario">Cargá el precio de compra</p>
          </>
        ) : (
          <>
            <p className="text-cifra-tarjeta font-titulos font-bold text-texto-secundario">—</p>
            <p className="text-cuerpo text-texto-secundario">Cargá precio de venta en "Editar cortes"</p>
          </>
        )}
      </Tarjeta>
    </div>
  )
}

function formatearPorcentaje(valor: number): string {
  return `${new Intl.NumberFormat('es-AR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(valor)} %`
}
