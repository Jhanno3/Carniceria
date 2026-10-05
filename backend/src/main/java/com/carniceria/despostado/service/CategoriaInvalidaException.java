package com.carniceria.despostado.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class CategoriaInvalidaException extends ApiException {
	public CategoriaInvalidaException(String categoria) {
		super("CATEGORIA_INVALIDA",
				"\"" + categoria + "\" no es una categoría válida (Novillo, Novillito, Vaquillona, Vaca, Toro o Ternero).",
				HttpStatus.BAD_REQUEST);
	}
}
