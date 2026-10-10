package com.carniceria.despostado.modelo;

import com.carniceria.despostado.entity.PerdidaEntity.Tipo;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dominio puro — misma idea que {@link EstimacionCalculator} pero para Hueso/Grasa/Merma
 * en vez de por corte: para cada tipo de pérdida, promedia qué % representó sobre el peso
 * de entrada en todo el historial, y lo aplica al peso de la media res nueva.
 */
public final class PerdidaEstimacionCalculator {

	private static final int ESCALA_KG = 3;

	private PerdidaEstimacionCalculator() {
	}

	public static Map<Tipo, BigDecimal> estimar(List<RegistroPerdidaHistorico> historico, BigDecimal pesoKgNuevo) {
		return historico.stream()
				.collect(Collectors.groupingBy(RegistroPerdidaHistorico::tipo))
				.entrySet().stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						entrada -> estimarTipo(entrada.getValue(), pesoKgNuevo)));
	}

	private static BigDecimal estimarTipo(List<RegistroPerdidaHistorico> registrosDelTipo, BigDecimal pesoKgNuevo) {
		BigDecimal sumaDePorcentajes = registrosDelTipo.stream()
				.map(r -> r.kg().divide(r.pesoKgEntrada(), new MathContext(10)))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal porcentajePromedio = sumaDePorcentajes.divide(
				BigDecimal.valueOf(registrosDelTipo.size()), new MathContext(10));

		return porcentajePromedio.multiply(pesoKgNuevo).setScale(ESCALA_KG, RoundingMode.HALF_UP);
	}
}
