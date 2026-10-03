package com.carniceria.despostado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Un corte y sus kilos — se reutiliza tanto en el request (`cortes`) como en la respuesta (`despostado`). */
public record CorteKgDto(@NotNull UUID corteId, @NotBlank String kg) {
}
