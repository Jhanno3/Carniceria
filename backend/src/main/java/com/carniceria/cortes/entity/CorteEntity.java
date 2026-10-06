package com.carniceria.cortes.entity;

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
@Table(name = "cortes")
public class CorteEntity {

	public enum Cuarto {
		Delantero, Trasero, Ambos
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String nombre;

	@Column(nullable = false, unique = true)
	private Integer plu;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Cuarto cuarto;

	@Column(name = "zona_mapa")
	private String zonaMapa;

	@Column(nullable = false)
	private boolean activo = true;

	/** $/kg, opcional (FR-501) — precio que se le cobra al cliente, no un costo de compra. */
	@Column(name = "precio_venta")
	private BigDecimal precioVenta;

	/** El negocio dueño de este corte (V10__multi_negocio.sql) — cada dueño tiene su propio catálogo. */
	@Column(name = "dueno_id", nullable = false)
	private UUID duenoId;

	protected CorteEntity() {
	}

	public CorteEntity(String nombre, Integer plu, Cuarto cuarto, String zonaMapa, boolean activo,
			BigDecimal precioVenta, UUID duenoId) {
		this.nombre = nombre;
		this.plu = plu;
		this.cuarto = cuarto;
		this.zonaMapa = zonaMapa;
		this.activo = activo;
		this.precioVenta = precioVenta;
		this.duenoId = duenoId;
	}

	public UUID getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Integer getPlu() {
		return plu;
	}

	public void setPlu(Integer plu) {
		this.plu = plu;
	}

	public Cuarto getCuarto() {
		return cuarto;
	}

	public void setCuarto(Cuarto cuarto) {
		this.cuarto = cuarto;
	}

	public String getZonaMapa() {
		return zonaMapa;
	}

	public void setZonaMapa(String zonaMapa) {
		this.zonaMapa = zonaMapa;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}

	public BigDecimal getPrecioVenta() {
		return precioVenta;
	}

	public void setPrecioVenta(BigDecimal precioVenta) {
		this.precioVenta = precioVenta;
	}

	public UUID getDuenoId() {
		return duenoId;
	}
}
