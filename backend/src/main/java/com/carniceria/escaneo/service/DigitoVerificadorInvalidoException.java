package com.carniceria.escaneo.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class DigitoVerificadorInvalidoException extends ApiException {
	public DigitoVerificadorInvalidoException() {
		super("DIGITO_VERIFICADOR_INVALIDO", "El código escaneado no es válido.", HttpStatus.BAD_REQUEST);
	}
}
