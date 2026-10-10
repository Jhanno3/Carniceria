package com.carniceria.despostado.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import com.carniceria.despostado.entity.PerdidaEntity.Tipo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Sin Spring, sin JPA — misma idea que EstimacionCalculatorTest, pero por tipo de pérdida. */
class PerdidaEstimacionCalculatorTest {

	@Test
	void promedioHistoricoEscaladoAlPesoNuevo() {
		List<RegistroPerdidaHistorico> historico = List.of(
				new RegistroPerdidaHistorico(Tipo.hueso, new BigDecimal("11.000"), new BigDecimal("100.000")), // 11 %
				new RegistroPerdidaHistorico(Tipo.grasa, new BigDecimal("6.000"), new BigDecimal("100.000")), // 6 %
				new RegistroPerdidaHistorico(Tipo.merma, new BigDecimal("2.000"), new BigDecimal("100.000"))); // 2 %

		Map<Tipo, BigDecimal> estimado = PerdidaEstimacionCalculator.estimar(historico, new BigDecimal("80.000"));

		assertThat(estimado.get(Tipo.hueso)).isEqualByComparingTo("8.800"); // 11 % de 80 kg
		assertThat(estimado.get(Tipo.grasa)).isEqualByComparingTo("4.800"); // 6 % de 80 kg
		assertThat(estimado.get(Tipo.merma)).isEqualByComparingTo("1.600"); // 2 % de 80 kg
	}

	@Test
	void sinHistorial_devuelveUnMapaVacio() {
		Map<Tipo, BigDecimal> estimado = PerdidaEstimacionCalculator.estimar(List.of(), new BigDecimal("100.000"));

		assertThat(estimado).isEmpty();
	}

	@Test
	void unTipoSinNingunRegistro_noApareceEnElMapa() {
		List<RegistroPerdidaHistorico> historico = List.of(
				new RegistroPerdidaHistorico(Tipo.hueso, new BigDecimal("11.000"), new BigDecimal("100.000")));

		Map<Tipo, BigDecimal> estimado = PerdidaEstimacionCalculator.estimar(historico, new BigDecimal("80.000"));

		assertThat(estimado).containsOnlyKeys(Tipo.hueso);
	}
}
