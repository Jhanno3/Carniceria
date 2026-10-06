package com.carniceria.reportes.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import com.carniceria.reportes.modelo.CalculadorPeriodo.Periodo;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Sin Spring, sin JPA. Ver plan-fase4.md, 3.4. */
class CalculadorPeriodoTest {

	@Test
	void dia_devuelveLaMismaFecha() {
		LocalDate fecha = LocalDate.of(2026, 3, 15);

		assertThat(CalculadorPeriodo.inicioDelBucket(fecha, Periodo.dia)).isEqualTo(fecha);
	}

	@Test
	void semana_conUnaFechaQueYaEsLunes_devuelveLaMisma() {
		LocalDate lunes = LocalDate.of(2024, 1, 1); // 1/1/2024 es lunes.

		assertThat(CalculadorPeriodo.inicioDelBucket(lunes, Periodo.semana)).isEqualTo(lunes);
	}

	@Test
	void semana_conUnDomingo_devuelveElLunesDeEsaMismaSemana() {
		LocalDate domingo = LocalDate.of(2024, 1, 7); // domingo de la semana que arrancó el lunes 1/1/2024.

		assertThat(CalculadorPeriodo.inicioDelBucket(domingo, Periodo.semana)).isEqualTo(LocalDate.of(2024, 1, 1));
	}

	@Test
	void mes_conElUltimoDiaDeUnMesDe31_devuelveElDiaUnoDeEseMes() {
		LocalDate finDeMes = LocalDate.of(2026, 10, 31);

		assertThat(CalculadorPeriodo.inicioDelBucket(finDeMes, Periodo.mes)).isEqualTo(LocalDate.of(2026, 10, 1));
	}
}
