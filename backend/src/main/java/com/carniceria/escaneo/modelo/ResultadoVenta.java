package com.carniceria.escaneo.modelo;

import java.math.BigDecimal;

/** Lo que devuelve {@link CalculadorVenta} — separado de {@link ResultadoDecodificacion} (Fase 5, FR-502). */
public sealed interface ResultadoVenta {

	record Exito(BigDecimal kg, BigDecimal importe) implements ResultadoVenta {
	}

	record PesoCero() implements ResultadoVenta {
	}

	/** {@code tipoValor = importe} sin {@code precioVenta} cargado: no hay forma de recuperar los kilos. */
	record FaltaPrecioVenta() implements ResultadoVenta {
	}
}
