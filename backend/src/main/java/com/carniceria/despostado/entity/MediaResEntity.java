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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medias_reses")
public class MediaResEntity {

	// Clasificación típica de Mercado de Liniers (V17__categoria_animal.sql) — para el
	// reporte "por categoría de animal" de Fase 4 (spec.md, nota de la sección 5.3).
	public enum Categoria {
		Novillo, Novillito, Vaquillona, Vaca, Toro, Ternero
	}

	// Fase 6, FR-601: qué parte de la media res llegó como entrada — MediaRes es "toda
	// entera, sin restricción" (default), el resto son cortes comerciales más chicos. El
	// mapeo de cada uno a los cortes del catálogo que habilita vive solo en el frontend
	// (fase6/tasks-fase6.md) — el backend no lo valida, es nada más una restricción de UI.
	// AchurasEmbutidos/Cerdo/Carne (Fase 7): no se ofrecen en el selector "Corte" de
	// Despostado — los pone el modal "Añadir stock" de Control diario por su cuenta
	// (tasks-fase7.md).
	public enum TipoEntrada {
		MediaRes, Delantero, Pecho, Parrillero, AsadoCompleto, Mocho, Rueda, AchurasEmbutidos, Cerdo, Carne
	}

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

	@Enumerated(EnumType.STRING)
	private Categoria categoria;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_entrada", nullable = false)
	private TipoEntrada tipoEntrada;

	@Column(name = "creado_por", nullable = false)
	private UUID creadoPor;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	protected MediaResEntity() {
	}

	public MediaResEntity(LocalDate fecha, String proveedor, BigDecimal pesoKg, BigDecimal precioKg,
			Categoria categoria, TipoEntrada tipoEntrada, UUID creadoPor, Instant creadoEn) {
		this.fecha = fecha;
		this.proveedor = proveedor;
		this.pesoKg = pesoKg;
		this.precioKg = precioKg;
		this.categoria = categoria;
		this.tipoEntrada = tipoEntrada;
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

	public Categoria getCategoria() {
		return categoria;
	}

	public void setCategoria(Categoria categoria) {
		this.categoria = categoria;
	}

	public TipoEntrada getTipoEntrada() {
		return tipoEntrada;
	}

	public void setTipoEntrada(TipoEntrada tipoEntrada) {
		this.tipoEntrada = tipoEntrada;
	}

	public UUID getCreadoPor() {
		return creadoPor;
	}

	public Instant getCreadoEn() {
		return creadoEn;
	}
}
