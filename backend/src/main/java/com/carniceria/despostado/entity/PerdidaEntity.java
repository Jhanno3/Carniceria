package com.carniceria.despostado.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "perdidas")
public class PerdidaEntity {

	// Nombres en minúscula a propósito: EnumType.STRING guarda name() tal cual,
	// y debe coincidir exacto con el check constraint de la migración V6.
	public enum Tipo {
		hueso, grasa, merma
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "media_res_id", nullable = false)
	private UUID mediaResId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Tipo tipo;

	@Column(nullable = false)
	private BigDecimal kg;

	protected PerdidaEntity() {
	}

	public PerdidaEntity(UUID mediaResId, Tipo tipo, BigDecimal kg) {
		this.mediaResId = mediaResId;
		this.tipo = tipo;
		this.kg = kg;
	}

	public UUID getId() {
		return id;
	}

	public UUID getMediaResId() {
		return mediaResId;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public BigDecimal getKg() {
		return kg;
	}
}
