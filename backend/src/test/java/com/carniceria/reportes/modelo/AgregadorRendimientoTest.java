package com.carniceria.reportes.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import com.carniceria.reportes.modelo.AgregadorRendimiento.Entrada;
import com.carniceria.reportes.modelo.AgregadorRendimiento.Resultado;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Sin Spring, sin JPA. Ver plan-fase4.md, 3.2: promedio ponderado por kilos, no simple. */
class AgregadorRendimientoTest {

	// A: 100 kg entrada, 90 kg vendible (90 % individual), precioKg 1000 (costoTotal 100.000).
	private static final Entrada ENTRADA_A = new Entrada(
			new BigDecimal("100.000"), new BigDecimal("1000.00"), new BigDecimal("90.000"));
	// B: 10 kg entrada, 5 kg vendible (50 % individual), precioKg 2000 (costoTotal 20.000).
	private static final Entrada ENTRADA_B = new Entrada(
			new BigDecimal("10.000"), new BigDecimal("2000.00"), new BigDecimal("5.000"));
	// C: 50 kg entrada, 40 kg vendible (80 % individual), sin precioKg cargado.
	private static final Entrada ENTRADA_C = new Entrada(new BigDecimal("50.000"), null, new BigDecimal("40.000"));

	@Test
	void rendimientoPromedioPorc_esPonderadoPorKilos_noElPromedioSimple() {
		// Promedio simple de 90 % y 50 % sería 70 %; ponderado: (90+5)/(100+10) = 86,36 %.
		Resultado resultado = AgregadorRendimiento.agregar(List.of(ENTRADA_A, ENTRADA_B));

		assertThat(resultado.cantidadEntradas()).isEqualTo(2);
		assertThat(resultado.rendimientoPromedioPorc()).isEqualByComparingTo("86.36");
	}

	@Test
	void costoKgVendiblePromedio_esPonderadoPorKilos_noElPromedioSimple() {
		// Promedio simple de 1.111 y 4.000 sería 2.556; ponderado: (100.000+20.000)/(90+5) = 1.263.
		Resultado resultado = AgregadorRendimiento.agregar(List.of(ENTRADA_A, ENTRADA_B));

		assertThat(resultado.costoKgVendiblePromedio()).isEqualByComparingTo("1263");
	}

	@Test
	void unaEntradaSinPrecioKg_cuentaParaElRendimientoPeroNoParaElCosto() {
		Resultado resultado = AgregadorRendimiento.agregar(List.of(ENTRADA_A, ENTRADA_C));

		// (90+40)/(100+50) * 100 = 86,67 % — C entra acá.
		assertThat(resultado.rendimientoPromedioPorc()).isEqualByComparingTo("86.67");
		// 100.000/90 = 1.111 — el costo de C no se suma ni su vendibleKg cuenta en el divisor.
		assertThat(resultado.costoKgVendiblePromedio()).isEqualByComparingTo("1111");
	}

	@Test
	void ningunaEntradaConPrecioKg_dejaElCostoEnNull() {
		Resultado resultado = AgregadorRendimiento.agregar(List.of(ENTRADA_C));

		assertThat(resultado.costoKgVendiblePromedio()).isNull();
	}
}
