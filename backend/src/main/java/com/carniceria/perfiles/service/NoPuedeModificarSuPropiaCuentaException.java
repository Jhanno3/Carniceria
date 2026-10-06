package com.carniceria.perfiles.service;

import com.carniceria.shared.error.ApiException;
import org.springframework.http.HttpStatus;

public class NoPuedeModificarSuPropiaCuentaException extends ApiException {
	public NoPuedeModificarSuPropiaCuentaException() {
		super("NO_PUEDE_MODIFICAR_SU_PROPIA_CUENTA", "No podés cambiar tu propia cuenta desde acá.",
				HttpStatus.FORBIDDEN);
	}
}
