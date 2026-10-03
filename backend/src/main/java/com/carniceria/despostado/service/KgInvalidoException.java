package com.carniceria.despostado.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class KgInvalidoException extends ApiException {
	public KgInvalidoException() {
		super("KG_INVALIDO", "Los kilos cargados deben ser mayores a cero.", HttpStatus.BAD_REQUEST);
	}
}
