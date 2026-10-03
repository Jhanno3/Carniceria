package com.carniceria.cortes.service;

import com.carniceria.shared.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CorteNoEncontradoException extends ApiException {
	public CorteNoEncontradoException(UUID id) {
		super("CORTE_NO_ENCONTRADO", "El corte " + id + " no existe.", HttpStatus.NOT_FOUND);
	}
}
