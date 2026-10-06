package com.carniceria.escaneo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ventas")
public class VentaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "fecha_hora", nullable = false)
	private Instant fechaHora;

	@Column(name = "corte_id", nullable = false)
	private UUID corteId;

	@Column(nullable = false)
	private BigDecimal kg;

	/** $ (Fase 5, FR-502/FR-503) — null si el corte no tenía precio_venta al venderse. */
	@Column(name = "precio_total")
	private BigDecimal precioTotal;

	@Column(name = "codigo_leido", nullable = false)
	private String codigoLeido;

	@Column(name = "id_cliente_local", nullable = false)
	private UUID idClienteLocal;

	@Column(nullable = false)
	private boolean anulada = false;

	// Quién escaneó (dueño o empleado) vs. a qué negocio pertenece la venta — ver
	// data-model-fase2.md: a diferencia de medias_reses, acá pueden ser personas distintas.
	@Column(name = "usuario_id", nullable = false)
	private UUID usuarioId;

	@Column(name = "dueno_id", nullable = false)
	private UUID duenoId;

	protected VentaEntity() {
	}

	public VentaEntity(UUID corteId, BigDecimal kg, BigDecimal precioTotal, String codigoLeido, UUID idClienteLocal,
			UUID usuarioId, UUID duenoId, Instant fechaHora) {
		this.corteId = corteId;
		this.kg = kg;
		this.precioTotal = precioTotal;
		this.codigoLeido = codigoLeido;
		this.idClienteLocal = idClienteLocal;
		this.usuarioId = usuarioId;
		this.duenoId = duenoId;
		this.fechaHora = fechaHora;
	}

	public UUID getId() {
		return id;
	}

	public Instant getFechaHora() {
		return fechaHora;
	}

	public UUID getCorteId() {
		return corteId;
	}

	public BigDecimal getKg() {
		return kg;
	}

	public BigDecimal getPrecioTotal() {
		return precioTotal;
	}

	public String getCodigoLeido() {
		return codigoLeido;
	}

	public UUID getIdClienteLocal() {
		return idClienteLocal;
	}

	public boolean isAnulada() {
		return anulada;
	}

	public UUID getUsuarioId() {
		return usuarioId;
	}

	public UUID getDuenoId() {
		return duenoId;
	}
}
