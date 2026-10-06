package com.carniceria.escaneo.modelo;

import java.math.BigDecimal;

/**
 * Tipo resultado, no excepción (mismo criterio que {@code EstimacionCalculator} de Fase 1):
 * el Service decide la traducción HTTP de cada variante, el dominio puro no sabe de HTTP.
 */
public sealed interface ResultadoDecodificacion {

	// El valor crudo, sin interpretar todavía si es peso o importe (Fase 5, FR-502): eso
	// pasó a decidirlo CalculadorVenta, una vez que el Service sabe qué corte es (necesita
	// su precioVenta). El chequeo de "valor cero" también se movió ahí — ver ResultadoVenta.
	record Exito(int plu, BigDecimal valor) implements ResultadoDecodificacion {
	}

	record DigitoVerificadorInvalido() implements ResultadoDecodificacion {
	}

	record PrefijoInvalido() implements ResultadoDecodificacion {
	}
}
