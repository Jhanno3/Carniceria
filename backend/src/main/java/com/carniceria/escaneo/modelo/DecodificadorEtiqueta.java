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

		// tipoValor == importe: la etiqueta codifica un importe, no un peso — sin un precio
		// por kg configurado (no existe ese campo hoy, ver data-model-fase2.md) no hay forma
		// de recuperar el peso real a partir de esto. Se trata igual que "peso" a falta de
		// una definición mejor; ninguna carnicería de referencia usa este modo todavía.
		BigDecimal kg = BigDecimal.valueOf(valorCrudo).movePointLeft(config.decimales());

		if (kg.signum() <= 0) {
			return new ResultadoDecodificacion.PesoCero();
		}
		return new ResultadoDecodificacion.Exito(plu, kg);
	}
}
