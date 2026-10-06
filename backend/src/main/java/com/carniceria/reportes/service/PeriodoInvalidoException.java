package com.carniceria.reportes.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class PeriodoInvalidoException extends ApiException {
	public PeriodoInvalidoException(String periodo) {
		super("PERIODO_INVALIDO", "\"" + periodo + "\" no es un período válido (dia, semana o mes).",
				HttpStatus.BAD_REQUEST);
	}
}
