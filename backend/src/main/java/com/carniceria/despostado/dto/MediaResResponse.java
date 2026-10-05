package com.carniceria.despostado.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MediaResResponse(
		UUID id,
		LocalDate fecha,
		String proveedor,
		String pesoKg,
		String precioKg,
		String categoria,
		List<CorteKgDto> despostado,
		PerdidasDto perdidas,
		ResumenDto resumen) {
}
