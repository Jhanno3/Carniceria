package com.carniceria.despostado.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Body de `POST /medias-reses` y `PUT /medias-reses/{id}` (mismo shape). */
public record CargarEntradaRequest(
		String proveedor,
		@NotBlank String pesoKg,
		String precioKg,
		@Valid List<CorteKgDto> cortes,
		PerdidasDto perdidas) {

	public List<CorteKgDto> cortesOVacio() {
		return cortes == null ? List.of() : cortes;
	}
}
