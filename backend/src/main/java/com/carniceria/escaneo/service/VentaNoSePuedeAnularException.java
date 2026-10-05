package com.carniceria.escaneo.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class VentaNoSePuedeAnularException extends ApiException {
	public VentaNoSePuedeAnularException() {
		super("VENTA_NO_SE_PUEDE_ANULAR", "No podés anular esta venta: es de otro usuario o pasaron más de 5 minutos.",
				HttpStatus.FORBIDDEN);
	}
}
