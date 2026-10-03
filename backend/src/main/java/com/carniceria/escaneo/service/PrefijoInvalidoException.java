package com.carniceria.escaneo.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class PrefijoInvalidoException extends ApiException {
	public PrefijoInvalidoException() {
		super("PREFIJO_INVALIDO", "El código escaneado no corresponde a una etiqueta de peso variable.",
				HttpStatus.BAD_REQUEST);
	}
}
