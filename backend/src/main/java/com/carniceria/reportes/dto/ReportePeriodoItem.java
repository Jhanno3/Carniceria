package com.carniceria.reportes.dto;

/** Ver contracts/reportes-api.md, "GET /reportes/por-periodo" (FR-402/FR-505). */
public record ReportePeriodoItem(
		String periodoInicio, int cantidadEntradas, String rendimientoPromedioPorc, String costoKgVendiblePromedio,
		String beneficioPorKgVendible) {
}
