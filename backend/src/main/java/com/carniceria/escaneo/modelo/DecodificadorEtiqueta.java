package com.carniceria.escaneo.modelo;

import java.math.BigDecimal;

/**
 * Decodifica un código EAN-13 de balanza según la {@link ConfigEtiqueta} del negocio que
 * escanea (plan-fase2.md, 3.1: vive solo acá, nunca en el frontend). No conoce nada de
 * `cortes` — si el PLU decodificado existe y está activo lo valida el Service (único lugar
 * con acceso al catálogo de ese negocio).
 */
public final class DecodificadorEtiqueta {

	private DecodificadorEtiqueta() {
	}

	public static ResultadoDecodificacion decodificar(String codigo, ConfigEtiqueta config) {
		if (!Ean13.validarDigitoVerificador(codigo)) {
			return new ResultadoDecodificacion.DigitoVerificadorInvalido();
		}

		int prefijo = Integer.parseInt(codigo.substring(0, 2));
		if (prefijo < config.prefijoDesde() || prefijo > config.prefijoHasta()) {
			return new ResultadoDecodificacion.PrefijoInvalido();
		}

		int plu = Integer.parseInt(codigo.substring(config.inicioPlu(), config.inicioPlu() + config.largoPlu()));
		long valorCrudo = Long.parseLong(
				codigo.substring(config.inicioValor(), config.inicioValor() + config.largoValor()));

		// El valor crudo, con la coma ya corrida según `decimales` — sin interpretar
		// todavía si es peso o importe (Fase 5, FR-502: eso lo decide CalculadorVenta, una
		// vez que el Service sabe el precioVenta del corte decodificado por `plu`).
		BigDecimal valor = BigDecimal.valueOf(valorCrudo).movePointLeft(config.decimales());
		return new ResultadoDecodificacion.Exito(plu, valor);
	}
}
