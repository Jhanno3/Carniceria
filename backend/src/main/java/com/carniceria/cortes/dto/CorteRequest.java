package com.carniceria.cortes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Mismo body para crear y editar un corte; {@code activo} se ignora al crear (siempre nace activo). */
public record CorteRequest(
		@NotBlank String nombre,
		@NotNull Integer plu,
		@NotBlank String cuarto,
		String zonaMapa,
		Boolean activo,
		// $/kg (FR-501): viaja como string, igual que precioKg en medias_reses — ver
		// BigDecimals. DecimalMin acepta CharSequence (null no se valida, pasa igual).
		@DecimalMin(value = "0", inclusive = false, message = "debe ser mayor a 0") String precioVenta) {
}
