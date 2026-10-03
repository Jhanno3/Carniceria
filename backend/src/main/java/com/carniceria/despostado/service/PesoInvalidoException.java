package com.carniceria.despostado.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class PesoInvalidoException extends ApiException {
	public PesoInvalidoException() {
		super("PESO_INVALIDO", "El peso de entrada debe ser mayor a cero.", HttpStatus.BAD_REQUEST);
	}
}
