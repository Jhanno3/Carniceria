package com.carniceria.despostado.service;

import com.carniceria.shared.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class MediaResNoEncontradaException extends ApiException {
	public MediaResNoEncontradaException(UUID id) {
		super("MEDIA_RES_NO_ENCONTRADA", "La entrada " + id + " no existe.", HttpStatus.NOT_FOUND);
	}
}
