package com.carniceria.escaneo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Mapea la vista {@code stock_por_corte} (V15), de solo lectura — {@code @Immutable} le
 * dice a Hibernate que nunca intente hacer dirty-checking ni UPDATE sobre esto (no tendría
 * sentido, es una vista). El aislamiento por negocio lo da la vista misma
 * (security_invoker + RLS de cortes/despostado/ventas), no hace falta nada acá.
 */
@Entity
@Immutable
@Table(name = "stock_por_corte")
public class StockPorCorteEntity {

	@Id
	@Column(name = "corte_id")
	private UUID corteId;

	@Column(name = "entrado_kg", nullable = false)
	private BigDecimal entradoKg;

	@Column(name = "vendido_kg", nullable = false)
	private BigDecimal vendidoKg;

	@Column(name = "stock_kg", nullable = false)
	private BigDecimal stockKg;

	protected StockPorCorteEntity() {
	}

	public UUID getCorteId() {
		return corteId;
	}

	public BigDecimal getEntradoKg() {
		return entradoKg;
	}

	public BigDecimal getVendidoKg() {
		return vendidoKg;
	}

	public BigDecimal getStockKg() {
		return stockKg;
	}
}
