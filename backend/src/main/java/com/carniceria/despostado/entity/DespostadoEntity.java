package com.carniceria.despostado.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "despostado")
public class DespostadoEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "media_res_id", nullable = false)
	private UUID mediaResId;

	@Column(name = "corte_id", nullable = false)
	private UUID corteId;

	@Column(nullable = false)
	private BigDecimal kg;

	protected DespostadoEntity() {
	}

	public DespostadoEntity(UUID mediaResId, UUID corteId, BigDecimal kg) {
		this.mediaResId = mediaResId;
		this.corteId = corteId;
		this.kg = kg;
	}

	public UUID getId() {
		return id;
	}

	public UUID getMediaResId() {
		return mediaResId;
	}

	public UUID getCorteId() {
		return corteId;
	}

	public BigDecimal getKg() {
		return kg;
	}
}
