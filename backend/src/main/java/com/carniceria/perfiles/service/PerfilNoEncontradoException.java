package com.carniceria.perfiles.service;

import com.carniceria.shared.error.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class PerfilNoEncontradoException extends ApiException {
	public PerfilNoEncontradoException(UUID id) {
		super("PERFIL_NO_ENCONTRADO", "El perfil " + id + " no existe.", HttpStatus.NOT_FOUND);
	}
}
