package com.carniceria.escaneo.dto;

/** Ver contracts/control-diario-api.md, "GET /control-diario/resumen" (FR-206/FR-503). */
public record ResumenDiaResponse(
		String kgVendidosHoy, int etiquetasEscaneadasHoy, String stockVendibleTotal, int entradasHoy,
		String dineroRecaudadoHoy, int ventasSinPrecioHoy) {
}
