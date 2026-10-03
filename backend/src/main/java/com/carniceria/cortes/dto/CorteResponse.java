package com.carniceria.cortes.dto;

import com.carniceria.cortes.entity.CorteEntity;
import java.util.UUID;

public record CorteResponse(UUID id, String nombre, Integer plu, String cuarto, String zonaMapa, boolean activo) {

	public static CorteResponse de(CorteEntity entity) {
		return new CorteResponse(
				entity.getId(),
				entity.getNombre(),
				entity.getPlu(),
				entity.getCuarto().name(),
				entity.getZonaMapa(),
				entity.isActivo());
	}
}
