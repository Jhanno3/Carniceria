package com.carniceria.despostado.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class SinHistorialException extends ApiException {
	public SinHistorialException() {
		super("SIN_HISTORIAL", "Todavía no hay ninguna entrada cargada para estimar.", HttpStatus.CONFLICT);
	}
}
