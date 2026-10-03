package com.carniceria.escaneo.dto;

import java.util.UUID;

/** Ver contracts/control-diario-api.md, "GET /stock" (FR-205, FR-208). */
public record StockCorteResponse(
		UUID corteId, String corteNombre, String entradoKg, String vendidoKg, String stockKg, boolean quedaPoco) {
}
