package com.carniceria.escaneo.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculadorVentaTest {

	@Test
	void tipoPeso_conPrecioVenta_calculaElImporte() {
		ResultadoVenta resultado = CalculadorVenta.calcular(
				new BigDecimal("1.250"), ConfigEtiqueta.TipoValor.peso, new BigDecimal("6500"));

		assertThat(resultado).isInstanceOf(ResultadoVenta.Exito.class);
		ResultadoVenta.Exito exito = (ResultadoVenta.Exito) resultado;
		assertThat(exito.kg()).isEqualByComparingTo(new BigDecimal("1.250"));
		assertThat(exito.importe()).isEqualByComparingTo(new BigDecimal("8125"));
	}

	@Test
	void tipoPeso_sinPrecioVenta_dejaElImporteNull() {
		ResultadoVenta resultado = CalculadorVenta.calcular(new BigDecimal("1.250"), ConfigEtiqueta.TipoValor.peso, null);

		assertThat(resultado).isInstanceOf(ResultadoVenta.Exito.class);
		ResultadoVenta.Exito exito = (ResultadoVenta.Exito) resultado;
		assertThat(exito.kg()).isEqualByComparingTo(new BigDecimal("1.250"));
		assertThat(exito.importe()).isNull();
	}

	@Test
	void tipoImporte_conPrecioVenta_calculaLosKilosRedondeandoHaciaArriba() {
		// 8120 / 6500 = 1,24923... -> HALF_UP a 3 decimales = 1,249 (no 1,250: prueba que redondea de verdad).
		ResultadoVenta resultado = CalculadorVenta.calcular(
				new BigDecimal("8120"), ConfigEtiqueta.TipoValor.importe, new BigDecimal("6500"));

		assertThat(resultado).isInstanceOf(ResultadoVenta.Exito.class);
		ResultadoVenta.Exito exito = (ResultadoVenta.Exito) resultado;
		assertThat(exito.kg()).isEqualByComparingTo(new BigDecimal("1.249"));
		assertThat(exito.importe()).isEqualByComparingTo(new BigDecimal("8120"));
	}

	@Test
	void tipoImporte_sinPrecioVenta_devuelveFaltaPrecioVenta() {
		ResultadoVenta resultado = CalculadorVenta.calcular(new BigDecimal("8125"), ConfigEtiqueta.TipoValor.importe, null);

		assertThat(resultado).isInstanceOf(ResultadoVenta.FaltaPrecioVenta.class);
	}

	@Test
	void valorCero_enModoPeso_devuelvePesoCero() {
		ResultadoVenta resultado = CalculadorVenta.calcular(
				BigDecimal.ZERO, ConfigEtiqueta.TipoValor.peso, new BigDecimal("6500"));

		assertThat(resultado).isInstanceOf(ResultadoVenta.PesoCero.class);
	}

	@Test
	void valorCero_enModoImporte_devuelvePesoCeroAntesDeDividir() {
		// Sin precioVenta Y valor cero a la vez: PesoCero gana, ni siquiera llega a evaluar
		// si falta el precio.
		ResultadoVenta resultado = CalculadorVenta.calcular(BigDecimal.ZERO, ConfigEtiqueta.TipoValor.importe, null);

		assertThat(resultado).isInstanceOf(ResultadoVenta.PesoCero.class);
	}
}
