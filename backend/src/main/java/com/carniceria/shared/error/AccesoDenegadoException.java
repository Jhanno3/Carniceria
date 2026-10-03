package com.carniceria.shared.error;

import org.springframework.http.HttpStatus;

public class AccesoDenegadoException extends ApiException {
	public AccesoDenegadoException() {
		super("ACCESO_DENEGADO", "No tenés permiso para hacer esto.", HttpStatus.FORBIDDEN);
	}
}
