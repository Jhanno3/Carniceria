package com.carniceria.escaneo.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class PesoCeroException extends ApiException {
	public PesoCeroException() {
		super("PESO_CERO", "El peso leído es cero.", HttpStatus.BAD_REQUEST);
	}
}
