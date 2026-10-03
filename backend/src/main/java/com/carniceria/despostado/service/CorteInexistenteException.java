package com.carniceria.despostado.service;

import com.carniceria.shared.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CorteInexistenteException extends ApiException {
	public CorteInexistenteException(UUID corteId) {
		super("CORTE_INEXISTENTE", "El corte " + corteId + " no existe o está inactivo.", HttpStatus.BAD_REQUEST);
	}
}
