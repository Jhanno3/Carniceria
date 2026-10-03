package com.carniceria.escaneo.modelo;

/**
 * Dominio puro — espejo de la fila `config_etiqueta` del propio negocio
 * (data-model-fase2.md), sin nada de JPA. La arma el Service a partir de la entity antes
 * de pasársela al decodificador.
 */
public record ConfigEtiqueta(
		int prefijoDesde,
		int prefijoHasta,
		int inicioPlu,
		int largoPlu,
		int inicioValor,
		int largoValor,
		TipoValor tipoValor,
		int decimales) {

	public enum TipoValor {
		peso, importe
	}
}
