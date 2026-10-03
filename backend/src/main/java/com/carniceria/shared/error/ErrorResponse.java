package com.carniceria.shared.error;

/** Formato de error común de la API (contracts/despostado-api.md, "Formato de error común"). */
public record ErrorResponse(String error, String mensaje) {
}
