package com.carniceria.cortes.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class DescuentaStockDeSiMismoException extends ApiException {
	public DescuentaStockDeSiMismoException() {
		super("DESCUENTA_STOCK_DE_SI_MISMO", "Un corte no puede descontar el stock de sí mismo.",
				HttpStatus.BAD_REQUEST);
	}
}
