package com.carniceria.despostado.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Sin Spring, sin JPA. Ver data-model.md, "Carga atómica y estimación automática". */
class EstimacionCalculatorTest {

	private static final UUID ASADO = UUID.randomUUID();
	private static final UUID VACIO = UUID.randomUUID();

	@Test
	void promedioHistoricoEscaladoAlPesoNuevo() {
		List<RegistroHistorico> historico = List.of(
				new RegistroHistorico(ASADO, new BigDecimal("10.000"), new BigDecimal("100.000")), // 10 %
				new RegistroHistorico(ASADO, new BigDecimal("20.000"), new BigDecimal("200.000")), // 10 %
				new RegistroHistorico(VACIO, new BigDecimal("3.000"), new BigDecimal("100.000"))); // 3 %

		Map<UUID, BigDecimal> estimado = EstimacionCalculator.estimar(historico, new BigDecimal("50.000"));

		assertThat(estimado.get(ASADO)).isEqualByComparingTo("5.000"); // 10 % de 50 kg
		assertThat(estimado.get(VACIO)).isEqualByComparingTo("1.500"); // 3 % de 50 kg
	}

	@Test
	void sinHistorial_devuelveUnMapaVacio() {
		Map<UUID, BigDecimal> estimado = EstimacionCalculator.estimar(List.of(), new BigDecimal("100.000"));

		assertThat(estimado).isEmpty();
	}

	@Test
	void unSoloRegistroHistorico_usaEsePorcentajeDirecto() {
		List<RegistroHistorico> historico = List.of(
				new RegistroHistorico(ASADO, new BigDecimal("11.000"), new BigDecimal("100.000")));

		Map<UUID, BigDecimal> estimado = EstimacionCalculator.estimar(historico, new BigDecimal("80.000"));

		assertThat(estimado.get(ASADO)).isEqualByComparingTo("8.800"); // 11 % de 80 kg
	}
}
