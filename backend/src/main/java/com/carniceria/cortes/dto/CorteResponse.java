package com.carniceria.cortes.dto;

import com.carniceria.cortes.entity.CorteEntity;
import com.carniceria.shared.BigDecimals;
import java.util.UUID;

public record CorteResponse(
		UUID id, String nombre, Integer plu, String cuarto, String tipoProducto, String zonaMapa, boolean activo,
		String precioVenta) {

	public static CorteResponse de(CorteEntity entity) {
		return new CorteResponse(
				entity.getId(),
				entity.getNombre(),
				entity.getPlu(),
				entity.getCuarto() == null ? null : entity.getCuarto().name(),
				entity.getTipoProducto().name(),
				entity.getZonaMapa(),
				entity.isActivo(),
				BigDecimals.aTexto(entity.getPrecioVenta()));
	}
}
