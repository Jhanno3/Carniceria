package com.carniceria.perfiles.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class ValorInvalidoException extends ApiException {
	public ValorInvalidoException(String campo, String valor) {
		super("VALOR_INVALIDO", "\"" + valor + "\" no es un valor válido para " + campo + ".",
				HttpStatus.BAD_REQUEST);
	}
}
