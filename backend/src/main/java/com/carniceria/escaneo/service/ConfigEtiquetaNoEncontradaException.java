package com.carniceria.escaneo.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class ConfigEtiquetaNoEncontradaException extends ApiException {
	public ConfigEtiquetaNoEncontradaException() {
		super("CONFIG_ETIQUETA_NO_ENCONTRADA", "Todavía no hay configuración de etiqueta para este negocio.",
				HttpStatus.NOT_FOUND);
	}
}
