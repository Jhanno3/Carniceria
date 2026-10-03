package com.carniceria.despostado.dto;

import com.carniceria.despostado.modelo.ResumenDespostado;
import com.carniceria.shared.BigDecimals;

public record ResumenDto(
		String vendibleKg,
		String perdidaKg,
		String sinAsignarKg,
		String rendimientoPorc,
		String costoTotal,
		String costoKgVendible) {

	public static ResumenDto de(ResumenDespostado resumen) {
		return new ResumenDto(
				BigDecimals.aTexto(resumen.vendibleKg()),
				BigDecimals.aTexto(resumen.perdidaKg()),
				BigDecimals.aTexto(resumen.sinAsignarKg()),
				BigDecimals.aTexto(resumen.rendimientoPorc()),
				BigDecimals.aTexto(resumen.costoTotal()),
				BigDecimals.aTexto(resumen.costoKgVendible()));
	}
}
