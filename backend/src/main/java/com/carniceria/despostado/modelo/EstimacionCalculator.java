package com.carniceria.despostado.modelo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dominio puro — ver data-model.md, "Carga atómica y estimación automática": para cada
 * corte, promedia qué % representó sobre el peso de entrada en todo el historial, y lo
 * aplica al peso de la media res nueva (FR-113 de spec.md).
 */
public final class EstimacionCalculator {

	private static final int ESCALA_KG = 3;

	private EstimacionCalculator() {
	}

	public static Map<UUID, BigDecimal> estimar(List<RegistroHistorico> historico, BigDecimal pesoKgNuevo) {
		return historico.stream()
				.collect(Collectors.groupingBy(RegistroHistorico::corteId))
				.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entrada -> estimarCorte(entrada.getValue(), pesoKgNuevo)));
	}

	private static BigDecimal estimarCorte(List<RegistroHistorico> registrosDelCorte, BigDecimal pesoKgNuevo) {
		BigDecimal sumaDePorcentajes = registrosDelCorte.stream()
				.map(r -> r.kg().divide(r.pesoKgEntrada(), new MathContext(10)))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal porcentajePromedio = sumaDePorcentajes.divide(
				BigDecimal.valueOf(registrosDelCorte.size()), new MathContext(10));

		return porcentajePromedio.multiply(pesoKgNuevo).setScale(ESCALA_KG, RoundingMode.HALF_UP);
	}
}
