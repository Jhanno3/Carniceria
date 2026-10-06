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
		/** MediaRes/Delantero/Pecho/Parrillero/AsadoCompleto/Mocho/Rueda — siempre presente (Fase 6, FR-601). */
		String tipoEntrada,
		List<CorteKgDto> despostado,
		PerdidasDto perdidas,
		ResumenDto resumen) {
}
