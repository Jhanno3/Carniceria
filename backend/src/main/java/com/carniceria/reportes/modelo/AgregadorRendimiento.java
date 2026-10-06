package com.carniceria.reportes.modelo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * Dominio puro — sin Spring ni JPA (plan-fase4.md, 3.2/3.5). Agrega el rendimiento de
 * varias medias reses (de un proveedor, una categoría, o un período) en un solo
 * resultado, ponderando por los kilos de cada una en vez de promediar los porcentajes
 * ya calculados — evita que una entrada chica pese lo mismo que una grande.
 */
public final class AgregadorRendimiento {

	private static final int ESCALA_PESOS = 0;
	private static final int ESCALA_PORCENTAJE = 2;
	private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

	private AgregadorRendimiento() {
	}

	/** Lo mínimo que hace falta de cada media res para agregar — ver ResumenDespostado (Fase 1). */
	public record Entrada(
			BigDecimal pesoKg, BigDecimal precioKg, BigDecimal vendibleKg,
			// Fase 5, FR-505: a diferencia de precioKg (null excluye TODA la entrada del
			// costo), acá null se trata como 0 al sumar — una media res puede tener algunos
			// cortes con precioVenta y otros sin él (DespostadoRepository ya filtró eso), así
			// que la entrada entera sigue aportando por la parte que sí tiene precio.
			BigDecimal kgConPrecioVenta, BigDecimal importeConPrecioVenta) {
	}

	public record Resultado(
			int cantidadEntradas, BigDecimal rendimientoPromedioPorc, BigDecimal costoKgVendiblePromedio,
			BigDecimal precioVentaPromedioPonderado, BigDecimal beneficioPorKgVendiblePromedio) {
	}

	public static Resultado agregar(List<Entrada> entradas) {
		BigDecimal pesoTotal = BigDecimal.ZERO;
		BigDecimal vendibleTotal = BigDecimal.ZERO;
		BigDecimal costoTotalSumado = BigDecimal.ZERO;
		BigDecimal vendibleConCostoTotal = BigDecimal.ZERO;
		BigDecimal kgConPrecioVentaTotal = BigDecimal.ZERO;
		BigDecimal importeConPrecioVentaTotal = BigDecimal.ZERO;

		for (Entrada entrada : entradas) {
			pesoTotal = pesoTotal.add(entrada.pesoKg());
			vendibleTotal = vendibleTotal.add(entrada.vendibleKg());
			if (entrada.precioKg() != null) {
				costoTotalSumado = costoTotalSumado.add(entrada.pesoKg().multiply(entrada.precioKg()));
				vendibleConCostoTotal = vendibleConCostoTotal.add(entrada.vendibleKg());
			}
			if (entrada.kgConPrecioVenta() != null) {
				kgConPrecioVentaTotal = kgConPrecioVentaTotal.add(entrada.kgConPrecioVenta());
			}
			if (entrada.importeConPrecioVenta() != null) {
				importeConPrecioVentaTotal = importeConPrecioVentaTotal.add(entrada.importeConPrecioVenta());
			}
		}

		BigDecimal rendimientoPromedioPorc = pesoTotal.signum() == 0
				? BigDecimal.ZERO.setScale(ESCALA_PORCENTAJE, REDONDEO)
				: vendibleTotal.multiply(BigDecimal.valueOf(100))
						.divide(pesoTotal, new MathContext(10))
						.setScale(ESCALA_PORCENTAJE, REDONDEO);

		BigDecimal costoKgVendiblePromedio = vendibleConCostoTotal.signum() == 0
				? null
				: costoTotalSumado.divide(vendibleConCostoTotal, new MathContext(10)).setScale(ESCALA_PESOS, REDONDEO);

		BigDecimal precioVentaPromedioPonderado = kgConPrecioVentaTotal.signum() == 0
				? null
				: importeConPrecioVentaTotal.divide(kgConPrecioVentaTotal, new MathContext(10))
						.setScale(ESCALA_PESOS, REDONDEO);

		BigDecimal beneficioPorKgVendiblePromedio = precioVentaPromedioPonderado == null || costoKgVendiblePromedio == null
				? null
				: precioVentaPromedioPonderado.subtract(costoKgVendiblePromedio);

		return new Resultado(entradas.size(), rendimientoPromedioPorc, costoKgVendiblePromedio,
				precioVentaPromedioPonderado, beneficioPorKgVendiblePromedio);
	}
}
