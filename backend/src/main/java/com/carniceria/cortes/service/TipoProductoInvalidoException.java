package com.carniceria.cortes.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class TipoProductoInvalidoException extends ApiException {
	public TipoProductoInvalidoException(String tipoProducto) {
		super("TIPO_PRODUCTO_INVALIDO",
				"\"" + tipoProducto + "\" no es un tipo de producto válido (Vacuno, AchurasEmbutidos o Cerdo).",
				HttpStatus.BAD_REQUEST);
	}
}
