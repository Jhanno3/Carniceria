package com.carniceria.perfiles.dto;

import com.carniceria.perfiles.entity.PerfilEntity;
import java.util.UUID;

public record PerfilResponse(UUID id, String nombre, String rol, String estado) {

	public static PerfilResponse de(PerfilEntity entity) {
		return new PerfilResponse(entity.getId(), entity.getNombre(), entity.getRol().name(),
				entity.getEstado().name());
	}
}
