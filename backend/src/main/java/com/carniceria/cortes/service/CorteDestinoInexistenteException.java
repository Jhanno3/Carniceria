package com.carniceria.cortes.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

/** El corte al que se quiere redirigir el stock no existe, no te pertenece, o está inactivo. */
public class CorteDestinoInexistenteException extends ApiException {
	public CorteDestinoInexistenteException() {
		super("CORTE_INEXISTENTE", "El corte al que querés descontar el stock no existe o no está activo.",
				HttpStatus.BAD_REQUEST);
	}
}
