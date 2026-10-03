package com.carniceria.despostado.modelo;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

/**
 * Dominio puro — sin anotaciones de Spring ni de JPA (research.md, "Arquitectura en
 * capas"). Implementa las fórmulas de la sección 6 de la especificación. No se persiste:
 * se reconstruye a partir de lo que ya está guardado en `medias_reses`/`despostado`/
 * `perdidas` cada vez que hace falta (data-model.md, "Valores calculados").
 */
public final class ResumenDespostado {

	private static final int ESCALA_KG = 3;
	// Los pesos argentinos se muestran sin centavos (especificacion-carniceria.md,
	// secciones 5.4 y 6: "$ 6.420", "$ 520.000" — ningún ejemplo de moneda tiene decimales).
	private static final int ESCALA_PESOS = 0;
	private static final int ESCALA_PORCENTAJE = 2;
	private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

	private final BigDecimal pesoKg;
	private final BigDecimal precioKg;
	private final Map<UUID, BigDecimal> kgPorCorte;
	private final Perdidas perdidas;

	public ResumenDespostado(BigDecimal pesoKg, BigDecimal precioKg, Map<UUID, BigDecimal> kgPorCorte,
			Perdidas perdidas) {
		if (pesoKg == null || pesoKg.signum() <= 0) {
			throw new IllegalArgumentException("El peso de entrada debe ser mayor a cero.");
		}
		this.pesoKg = pesoKg;
		this.precioKg = precioKg;
		this.kgPorCorte = Map.copyOf(kgPorCorte);
		this.perdidas = perdidas;
	}

	public BigDecimal vendibleKg() {
		return kgPorCorte.values().stream()
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(ESCALA_KG, REDONDEO);
	}

	public BigDecimal perdidaKg() {
		return perdidas.totalKg().setScale(ESCALA_KG, REDONDEO);
	}

	public BigDecimal sinAsignarKg() {
		return pesoKg.subtract(vendibleKg()).subtract(perdidaKg()).setScale(ESCALA_KG, REDONDEO);
	}

	public BigDecimal rendimientoPorc() {
		return vendibleKg()
				.multiply(BigDecimal.valueOf(100))
				.divide(pesoKg, new MathContext(10))
				.setScale(ESCALA_PORCENTAJE, REDONDEO);
	}

	public BigDecimal costoTotal() {
		if (precioKg == null) {
			return null;
		}
		return pesoKg.multiply(precioKg).setScale(ESCALA_PESOS, REDONDEO);
	}

	public BigDecimal costoKgVendible() {
		BigDecimal costoTotal = costoTotal();
		BigDecimal vendibleKg = vendibleKg();
		if (costoTotal == null || vendibleKg.signum() == 0) {
			return null;
		}
		return costoTotal.divide(vendibleKg, new MathContext(10)).setScale(ESCALA_PESOS, REDONDEO);
	}
}
