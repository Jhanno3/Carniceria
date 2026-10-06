package com.carniceria.escaneo.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DecodificadorEtiquetaTest {

	// especificacion-carniceria.md, sección 7: prefijo 20-29, PLU en 2-6, valor en 7-11,
	// peso en gramos (3 decimales) — mismo ejemplo que sembraría CatalogoInicialService.
	private static final ConfigEtiqueta CONFIG_DE_EJEMPLO = new ConfigEtiqueta(
			20, 29, 2, 5, 7, 5, ConfigEtiqueta.TipoValor.peso, 3);

	@Test
	void codigoDeLaEspecificacion_decodificaPluYValor() {
		ResultadoDecodificacion resultado = DecodificadorEtiqueta.decodificar("2000012012501", CONFIG_DE_EJEMPLO);

		assertThat(resultado).isInstanceOf(ResultadoDecodificacion.Exito.class);
		ResultadoDecodificacion.Exito exito = (ResultadoDecodificacion.Exito) resultado;
		assertThat(exito.plu()).isEqualTo(12);
		assertThat(exito.valor()).isEqualByComparingTo(new BigDecimal("1.250"));
	}

	@Test
	void digitoVerificadorInvalido_devuelveError() {
		ResultadoDecodificacion resultado = DecodificadorEtiqueta.decodificar("2000012012509", CONFIG_DE_EJEMPLO);

		assertThat(resultado).isInstanceOf(ResultadoDecodificacion.DigitoVerificadorInvalido.class);
	}

	@Test
	void largoDistintoDe13_esDigitoVerificadorInvalido_sinExcepcion() {
		ResultadoDecodificacion resultado = DecodificadorEtiqueta.decodificar("12345", CONFIG_DE_EJEMPLO);

		assertThat(resultado).isInstanceOf(ResultadoDecodificacion.DigitoVerificadorInvalido.class);
	}

	@Test
	void prefijoFueraDeRango_devuelveError() {
		// "190003405000" + dígito verificador 4 — prefijo 19, fuera de [20,29].
		ResultadoDecodificacion resultado = DecodificadorEtiqueta.decodificar("1900034050004", CONFIG_DE_EJEMPLO);

		assertThat(resultado).isInstanceOf(ResultadoDecodificacion.PrefijoInvalido.class);
	}

	@Test
	void valorEnCero_sigueSiendoExito_elChequeoDePesoCeroSeMovioACalculadorVenta() {
		// "200001200000" + dígito verificador 3 — PLU 12, valor 00000. El decodificador ya
		// no decide si 0 es un error (Fase 5, FR-502: eso ahora es CalculadorVenta.PesoCero).
		ResultadoDecodificacion resultado = DecodificadorEtiqueta.decodificar("2000012000003", CONFIG_DE_EJEMPLO);

		assertThat(resultado).isInstanceOf(ResultadoDecodificacion.Exito.class);
		assertThat(((ResultadoDecodificacion.Exito) resultado).valor()).isEqualByComparingTo(BigDecimal.ZERO);
	}
}
