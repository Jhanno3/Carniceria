package com.carniceria.cortes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Mismo body para crear y editar un corte; {@code activo} se ignora al crear (siempre nace activo). */
public record CorteRequest(
		@NotBlank String nombre,
		@NotNull Integer plu,
		@NotBlank String cuarto,
		String zonaMapa,
		Boolean activo) {
}
