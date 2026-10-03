package com.carniceria.despostado.dto;

import com.carniceria.despostado.modelo.Perdidas;
import com.carniceria.shared.BigDecimals;

public record PerdidasDto(String hueso, String grasa, String merma) {

	public static PerdidasDto de(Perdidas perdidas) {
		return new PerdidasDto(
				BigDecimals.aTexto(perdidas.hueso()),
				BigDecimals.aTexto(perdidas.grasa()),
				BigDecimals.aTexto(perdidas.merma()));
	}

	public Perdidas aPerdidas() {
		return new Perdidas(BigDecimals.parse(hueso), BigDecimals.parse(grasa), BigDecimals.parse(merma));
	}
}
