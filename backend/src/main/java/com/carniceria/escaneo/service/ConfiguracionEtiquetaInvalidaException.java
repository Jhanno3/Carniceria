package com.carniceria.escaneo.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class ConfiguracionEtiquetaInvalidaException extends ApiException {
	public ConfiguracionEtiquetaInvalidaException() {
		super("CONFIGURACION_ETIQUETA_INVALIDA",
				"Los rangos de prefijo, PLU y valor no son válidos o se superponen.", HttpStatus.BAD_REQUEST);
	}
}
