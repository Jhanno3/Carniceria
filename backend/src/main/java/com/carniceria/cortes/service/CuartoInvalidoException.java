package com.carniceria.cortes.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class CuartoInvalidoException extends ApiException {
	public CuartoInvalidoException(String cuarto) {
		super("CUARTO_INVALIDO", "\"" + cuarto + "\" no es un cuarto válido (Delantero, Trasero o Ambos).",
				HttpStatus.BAD_REQUEST);
	}
}
