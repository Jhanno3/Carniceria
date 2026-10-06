package com.carniceria.escaneo.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class CorteSinPrecioVentaException extends ApiException {
	public CorteSinPrecioVentaException() {
		super("CORTE_SIN_PRECIO_VENTA",
				"Este corte no tiene precio de venta cargado: no se puede calcular el peso a partir del importe.",
				HttpStatus.BAD_REQUEST);
	}
}
