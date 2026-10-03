package com.carniceria.perfiles.dto;

import jakarta.validation.constraints.NotBlank;

/** Body de `PUT /perfiles/{id}` — solo el dueño puede usarlo (RLS). */
public record PerfilActualizarRequest(@NotBlank String rol, @NotBlank String estado) {
}
