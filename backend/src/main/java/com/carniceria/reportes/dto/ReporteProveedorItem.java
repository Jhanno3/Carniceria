package com.carniceria.reportes.dto;

/** Ver contracts/reportes-api.md, "GET /reportes/por-proveedor" (FR-401/FR-505). */
public record ReporteProveedorItem(
		String proveedor, int cantidadEntradas, String rendimientoPromedioPorc, String costoKgVendiblePromedio,
		String beneficioPorKgVendible) {
}
