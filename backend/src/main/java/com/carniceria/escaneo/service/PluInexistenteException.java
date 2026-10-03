package com.carniceria.escaneo.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class PluInexistenteException extends ApiException {
	public PluInexistenteException() {
		super("PLU_INEXISTENTE", "El código escaneado no corresponde a ningún corte activo.", HttpStatus.BAD_REQUEST);
	}
}
