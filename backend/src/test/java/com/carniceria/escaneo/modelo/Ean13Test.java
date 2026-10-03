package com.carniceria.escaneo.modelo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Ean13Test {

	@Test
	void codigoDeLaEspecificacion_esValido() {
		// especificacion-carniceria.md, sección 7: PLU 12 (Vacío), 1,250 kg.
		assertThat(Ean13.validarDigitoVerificador("2000012012501")).isTrue();
	}

	@Test
	void mismoCodigoConDigitoVerificadorAdulterado_esInvalido() {
		assertThat(Ean13.validarDigitoVerificador("2000012012502")).isFalse();
	}

	@Test
	void otroCodigoValidoArmadoAMano() {
		// 20 00034 05000: primeros 12 dígitos 200003405000, suma = 2+0*3+0+0*3+0+3*3+4+0*3+5+0*3+0+0*3 = 20 → dígito 0.
		assertThat(Ean13.validarDigitoVerificador("2000034050000")).isTrue();
	}

	@Test
	void largoDistintoDe13_esInvalidoSinExcepcion() {
		assertThat(Ean13.validarDigitoVerificador("12345")).isFalse();
		assertThat(Ean13.validarDigitoVerificador("")).isFalse();
	}

	@Test
	void conCaracteresNoNumericos_esInvalidoSinExcepcion() {
		assertThat(Ean13.validarDigitoVerificador("20000A2012501")).isFalse();
	}

	@Test
	void nulo_esInvalidoSinExcepcion() {
		assertThat(Ean13.validarDigitoVerificador(null)).isFalse();
	}
}
