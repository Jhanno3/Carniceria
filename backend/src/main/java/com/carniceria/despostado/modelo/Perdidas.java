package com.carniceria.despostado.modelo;

import java.math.BigDecimal;

/** Las tres categorías de pérdida de la especificación (sección 8.1: hueso, grasa, merma). */
public record Perdidas(BigDecimal hueso, BigDecimal grasa, BigDecimal merma) {

	public Perdidas {
		hueso = hueso == null ? BigDecimal.ZERO : hueso;
		grasa = grasa == null ? BigDecimal.ZERO : grasa;
		merma = merma == null ? BigDecimal.ZERO : merma;
	}

	public BigDecimal totalKg() {
		return hueso.add(grasa).add(merma);
	}
}
