package com.carniceria.escaneo.dto;

import com.carniceria.escaneo.entity.VentaEntity;
import com.carniceria.shared.BigDecimals;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Ver contracts/control-diario-api.md: mismo shape para la respuesta de POST y cada fila de GET /ventas. */
public record VentaResponse(
		UUID id, String fechaHora, UUID corteId, String corteNombre, String kg, String precioTotal,
		String codigoLeido, boolean anulada, UUID usuarioId) {

	// constitution.md, Principio V: toda fecha de este negocio es en hora Argentina.
	private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

	public static VentaResponse de(VentaEntity entity, String corteNombre) {
		String fechaHora = OffsetDateTime.ofInstant(entity.getFechaHora(), ZONA_ARGENTINA)
				.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		return new VentaResponse(entity.getId(), fechaHora, entity.getCorteId(), corteNombre,
				BigDecimals.aTexto(entity.getKg()), BigDecimals.aTexto(entity.getPrecioTotal()),
				entity.getCodigoLeido(), entity.isAnulada(), entity.getUsuarioId());
	}
}
