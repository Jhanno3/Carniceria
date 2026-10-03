package com.carniceria.despostado.modelo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Sin Spring, sin JPA: solo JUnit. Reproduce el ejemplo obligatorio de la especificación
 * (sección 6): 100 kg a $5.200/kg, 81 kg vendibles -> $6.420 por kg vendible.
 */
class ResumenDespostadoTest {

	private static final UUID UN_CORTE = UUID.randomUUID();

	@Test
	void ejemploDeLaEspecificacion_100kgA5200_dan81kgVendiblesY6420PorKgVendible() {
		ResumenDespostado resumen = new ResumenDespostado(
				new BigDecimal("100.000"),
				new BigDecimal("5200.00"),
				Map.of(UN_CORTE, new BigDecimal("81.000")),
				new Perdidas(new BigDecimal("11.000"), new BigDecimal("6.000"), new BigDecimal("2.000")));

		assertThat(resumen.vendibleKg()).isEqualByComparingTo("81.000");
		assertThat(resumen.perdidaKg()).isEqualByComparingTo("19.000");
		assertThat(resumen.sinAsignarKg()).isEqualByComparingTo("0.000");
		assertThat(resumen.rendimientoPorc()).isEqualByComparingTo("81.00");
		assertThat(resumen.costoTotal()).isEqualByComparingTo("520000.00");
		assertThat(resumen.costoKgVendible()).isEqualByComparingTo("6420.00");
	}

	@Test
	void sinPrecioDeCompra_costoTotalYCostoKgVendibleSonNulos() {
		ResumenDespostado resumen = new ResumenDespostado(
				new BigDecimal("100.000"),
				null,
				Map.of(UN_CORTE, new BigDecimal("81.000")),
				new Perdidas(new BigDecimal("11.000"), new BigDecimal("6.000"), new BigDecimal("2.000")));

		assertThat(resumen.costoTotal()).isNull();
		assertThat(resumen.costoKgVendible()).isNull();
	}

	@Test
	void faltanKilosPorAsignar_sinAsignarKgEsPositivo() {
		ResumenDespostado resumen = new ResumenDespostado(
				new BigDecimal("100.000"),
				new BigDecimal("5200.00"),
				Map.of(UN_CORTE, new BigDecimal("79.000")),
				new Perdidas(new BigDecimal("11.000"), new BigDecimal("6.000"), new BigDecimal("2.000")));

		// 100 - 79 - 19 = 2 kg sin asignar
		assertThat(resumen.sinAsignarKg()).isEqualByComparingTo("2.000");
	}

	@Test
	void sobranKilos_sinAsignarKgEsNegativo() {
		ResumenDespostado resumen = new ResumenDespostado(
				new BigDecimal("100.000"),
				new BigDecimal("5200.00"),
				Map.of(UN_CORTE, new BigDecimal("83.000")),
				new Perdidas(new BigDecimal("11.000"), new BigDecimal("6.000"), new BigDecimal("2.000")));

		// 100 - 83 - 19 = -2 kg (sobran 2 kg)
		assertThat(resumen.sinAsignarKg()).isEqualByComparingTo("-2.000");
	}

	@Test
	void sinKilosVendibles_costoKgVendibleEsNulo() {
		ResumenDespostado resumen = new ResumenDespostado(
				new BigDecimal("100.000"),
				new BigDecimal("5200.00"),
				Map.of(),
				new Perdidas(new BigDecimal("0.000"), new BigDecimal("0.000"), new BigDecimal("0.000")));

		assertThat(resumen.vendibleKg()).isEqualByComparingTo("0.000");
		assertThat(resumen.costoKgVendible()).isNull();
	}

	@Test
	void pesoDeEntradaInvalido_rechazaLaConstruccion() {
		assertThatThrownBy(() -> new ResumenDespostado(
				new BigDecimal("0.000"),
				new BigDecimal("5200.00"),
				Map.of(),
				new Perdidas(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
