package com.carniceria.reportes.dto;

/** Ver contracts/reportes-api.md, "GET /reportes/por-categoria" (FR-403). */
public record ReporteCategoriaItem(
		String categoria, int cantidadEntradas, String rendimientoPromedioPorc, String costoKgVendiblePromedio) {
}
