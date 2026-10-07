package com.carniceria.cortes.service;

import com.carniceria.cortes.entity.CorteEntity.TipoProducto;
import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class CuartoNoAplicaException extends ApiException {
	public CuartoNoAplicaException(TipoProducto tipoProducto) {
		super("CUARTO_NO_APLICA",
				"El cuarto no aplica para \"" + tipoProducto + "\" — solo se especifica para cortes \"Vacuno\".",
				HttpStatus.BAD_REQUEST);
	}
}
