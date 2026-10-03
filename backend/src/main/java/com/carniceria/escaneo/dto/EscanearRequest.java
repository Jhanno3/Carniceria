package com.carniceria.escaneo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Ver contracts/control-diario-api.md, "POST /ventas". */
public record EscanearRequest(@NotBlank String codigo, @NotNull UUID idClienteLocal) {
}
