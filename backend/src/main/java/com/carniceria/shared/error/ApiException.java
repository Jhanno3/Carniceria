package com.carniceria.shared.error;

import org.springframework.http.HttpStatus;

/** Excepción base de negocio. Cada feature define las suyas extendiendo esta clase. */
public class ApiException extends RuntimeException {

	private final String codigo;
	private final HttpStatus status;

	public ApiException(String codigo, String mensaje, HttpStatus status) {
		super(mensaje);
		this.codigo = codigo;
		this.status = status;
	}

	public String getCodigo() {
		return codigo;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
