package com.carniceria.cortes.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Mismo body para crear y editar un corte; {@code activo} se ignora al crear (siempre nace activo).
 * {@code cuarto} ya no lleva {@code @NotBlank}: es obligatorio solo cuando {@code tipoProducto} es
 * "Vacuno" (o se omite, que es el default) — esa regla la valida {@code CorteService}, no Bean
 * Validation, porque depende de otro campo del mismo body (Fase 7). */
public record CorteRequest(
		@NotBlank String nombre,
		@NotNull Integer plu,
		String cuarto,
		// Opcional — ausente/blank se trata como "Vacuno" (mismo criterio permisivo que
		// tipoEntrada en medias_reses, Fase 6).
		String tipoProducto,
		String zonaMapa,
		Boolean activo,
		// $/kg (FR-501): viaja como string, igual que precioKg en medias_reses — ver
		// BigDecimals. DecimalMin acepta CharSequence (null no se valida, pasa igual).
		@DecimalMin(value = "0", inclusive = false, message = "debe ser mayor a 0") String precioVenta,
		// Opcional (Fase 8): si viene, este corte se vende con su propio precioVenta pero
		// el stock que se descuenta es el del corte referenciado acá, no el propio.
		UUID descuentaStockDeCorteId) {
}
