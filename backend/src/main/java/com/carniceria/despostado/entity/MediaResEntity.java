package com.carniceria.despostado.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medias_reses")
public class MediaResEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private LocalDate fecha;

	private String proveedor;

	@Column(name = "peso_kg", nullable = false)
	private BigDecimal pesoKg;

	@Column(name = "precio_kg")
	private BigDecimal precioKg;

	@Column(name = "creado_por", nullable = false)
	private UUID creadoPor;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	protected MediaResEntity() {
	}

	public MediaResEntity(LocalDate fecha, String proveedor, BigDecimal pesoKg, BigDecimal precioKg,
			UUID creadoPor, Instant creadoEn) {
		this.fecha = fecha;
		this.proveedor = proveedor;
		this.pesoKg = pesoKg;
		this.precioKg = precioKg;
		this.creadoPor = creadoPor;
		this.creadoEn = creadoEn;
	}

	public UUID getId() {
		return id;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public String getProveedor() {
		return proveedor;
	}

	public void setProveedor(String proveedor) {
		this.proveedor = proveedor;
	}

	public BigDecimal getPesoKg() {
		return pesoKg;
	}

	public void setPesoKg(BigDecimal pesoKg) {
		this.pesoKg = pesoKg;
	}

	public BigDecimal getPrecioKg() {
		return precioKg;
	}

	public void setPrecioKg(BigDecimal precioKg) {
		this.precioKg = precioKg;
	}

	public UUID getCreadoPor() {
		return creadoPor;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}
}
