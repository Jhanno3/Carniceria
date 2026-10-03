package com.carniceria.escaneo.modelo;

/**
 * Dígito verificador EAN-13, algoritmo GS1 estándar (plan-fase2.md, 3.2): sobre los
 * primeros 12 dígitos, los de posición impar (1ª, 3ª, ...) se suman tal cual y los de
 * posición par ×3; el dígito verificador es lo que le falta a esa suma para llegar al
 * siguiente múltiplo de 10.
 */
public final class Ean13 {

	private Ean13() {
	}

	public static boolean validarDigitoVerificador(String codigo) {
		if (codigo == null || codigo.length() != 13 || !soloDigitos(codigo)) {
			return false;
		}
		int suma = 0;
		for (int i = 0; i < 12; i++) {
			int digito = codigo.charAt(i) - '0';
			suma += (i % 2 == 0) ? digito : digito * 3;
		}
		int esperado = (10 - (suma % 10)) % 10;
		int real = codigo.charAt(12) - '0';
		return esperado == real;
	}

	private static boolean soloDigitos(String texto) {
		for (int i = 0; i < texto.length(); i++) {
			if (!Character.isDigit(texto.charAt(i))) {
				return false;
			}
		}
		return true;
	}
}
