package com.carniceria.escaneo.modelo;

import java.math.BigDecimal;

/**
 * Tipo resultado, no excepción (mismo criterio que {@code EstimacionCalculator} de Fase 1):
 * el Service decide la traducción HTTP de cada variante, el dominio puro no sabe de HTTP.
 */
public sealed interface ResultadoDecodificacion {

	record Exito(int plu, BigDecimal kg) implements ResultadoDecodificacion {
	}

	record DigitoVerificadorInvalido() implements ResultadoDecodificacion {
	}

	record PrefijoInvalido() implements ResultadoDecodificacion {
	}

	record PesoCero() implements ResultadoDecodificacion {
	}
}
