package com.carniceria.cortes.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class PluDuplicadoException extends ApiException {
	public PluDuplicadoException(Integer plu) {
		super("PLU_DUPLICADO", "Ya existe un corte con el PLU " + plu + ".", HttpStatus.CONFLICT);
	}
}
