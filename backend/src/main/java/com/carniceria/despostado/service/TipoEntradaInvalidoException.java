package com.carniceria.despostado.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class TipoEntradaInvalidoException extends ApiException {
	public TipoEntradaInvalidoException(String tipoEntrada) {
		super("TIPO_ENTRADA_INVALIDO",
				"\"" + tipoEntrada + "\" no es un tipo de entrada válido (MediaRes, Delantero, Pecho, "
						+ "Parrillero, AsadoCompleto, Mocho o Rueda).",
				HttpStatus.BAD_REQUEST);
	}
}
