package com.carniceria.escaneo.service;

import com.carniceria.shared.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class VentaNoEncontradaException extends ApiException {
	public VentaNoEncontradaException(UUID id) {
		super("VENTA_NO_ENCONTRADA", "La venta " + id + " no existe o no es de tu negocio.", HttpStatus.NOT_FOUND);
	}
}
