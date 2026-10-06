package com.carniceria.escaneo.modelo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Deriva, a partir del valor crudo que trae la etiqueta, el valor que no trae directamente
 * (Fase 5, FR-502): {@code tipoValor = peso} → calcula el importe; {@code tipoValor =
 * importe} → calcula los kilos. Dominio puro, sin Spring ni JPA (mismo criterio que
 * {@link DecodificadorEtiqueta}) — el Service es quien sabe el {@code precioVenta} del corte.
 */
public final class CalculadorVenta {

	private static final int ESCALA_KG = 3;
	private static final int ESCALA_IMPORTE = 0;
	private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

	private CalculadorVenta() {
	}

	public static ResultadoVenta calcular(BigDecimal valor, ConfigEtiqueta.TipoValor tipoValor, BigDecimal precioVentaCorte) {
		if (valor.signum() <= 0) {
			return new ResultadoVenta.PesoCero();
		}

		return switch (tipoValor) {
			case peso -> {
				BigDecimal kg = valor.setScale(ESCALA_KG, REDONDEO);
				BigDecimal importe = precioVentaCorte == null
						? null
						: kg.multiply(precioVentaCorte).setScale(ESCALA_IMPORTE, REDONDEO);
				yield new ResultadoVenta.Exito(kg, importe);
			}
			case importe -> {
				if (precioVentaCorte == null) {
					yield new ResultadoVenta.FaltaPrecioVenta();
				}
				BigDecimal importe = valor.setScale(ESCALA_IMPORTE, REDONDEO);
				BigDecimal kg = valor.divide(precioVentaCorte, new MathContext(10)).setScale(ESCALA_KG, REDONDEO);
				yield new ResultadoVenta.Exito(kg, importe);
			}
		};
	}
}
