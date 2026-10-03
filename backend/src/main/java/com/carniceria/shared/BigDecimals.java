package com.carniceria.shared;

import java.math.BigDecimal;

/**
 * Kilos e importes viajan por la API como string, nunca como number de JSON
 * (contracts/despostado-api.md), para no depender de qué motor de serialización use
 * Spring por dentro (Spring Boot 4 pasó a Jackson 3, ver research.md). Acá se hace la
 * conversión a mano, explícita, en los dos sentidos.
 */
public final class BigDecimals {

	private BigDecimals() {
	}

	public static BigDecimal parse(String valor) {
		return (valor == null || valor.isBlank()) ? null : new BigDecimal(valor);
	}

	public static String aTexto(BigDecimal valor) {
		return valor == null ? null : valor.toPlainString();
	}
}
