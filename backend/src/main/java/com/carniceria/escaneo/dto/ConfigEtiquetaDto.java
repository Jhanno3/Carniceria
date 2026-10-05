package com.carniceria.escaneo.dto;

import com.carniceria.escaneo.entity.ConfigEtiquetaEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Ver contracts/control-diario-api.md, "GET/PUT /config-etiqueta" (FR-209). Mismo shape para los dos. */
public record ConfigEtiquetaDto(
		@NotNull Integer prefijoDesde,
		@NotNull Integer prefijoHasta,
		@NotNull Integer inicioPlu,
		@NotNull Integer largoPlu,
		@NotNull Integer inicioValor,
		@NotNull Integer largoValor,
		@NotBlank String tipoValor,
		@NotNull Integer decimales) {

	public static ConfigEtiquetaDto de(ConfigEtiquetaEntity entity) {
		return new ConfigEtiquetaDto(
				entity.getPrefijoDesde(), entity.getPrefijoHasta(),
				entity.getInicioPlu(), entity.getLargoPlu(),
				entity.getInicioValor(), entity.getLargoValor(),
				entity.getTipoValor().name(), entity.getDecimales());
	}
}
