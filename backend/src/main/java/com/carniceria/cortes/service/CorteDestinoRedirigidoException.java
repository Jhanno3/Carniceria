package com.carniceria.cortes.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

/** Solo un nivel: el corte de destino no puede a su vez descontar el stock de otro corte. */
public class CorteDestinoRedirigidoException extends ApiException {
	public CorteDestinoRedirigidoException() {
		super("CORTE_DESTINO_REDIRIGIDO",
				"Ese corte ya descuenta su stock de otro — no se pueden encadenar.",
				HttpStatus.BAD_REQUEST);
	}
}
