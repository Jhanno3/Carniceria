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

	/** Vacuno (default) = sale de despostar una media res, usa {@code cuarto}. Los otros tres
	 * se compran ya terminados (Fase 7, Carne agregado por feedback de la misma fase) — para
	 * esos, {@code cuarto} siempre es {@code null}. */
	public enum TipoProducto {
		Vacuno, AchurasEmbutidos, Cerdo, Carne
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String nombre;

	@Column(nullable = false, unique = true)
	private Integer plu;

	// Nullable: solo aplica cuando tipoProducto = Vacuno (Fase 7).
	@Enumerated(EnumType.STRING)
	private Cuarto cuarto;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_producto", nullable = false)
	private TipoProducto tipoProducto = TipoProducto.Vacuno;

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

	/** Sin relación @ManyToOne a propósito (mismo criterio que el resto del repo) — id
	 * explícito de otro corte del mismo catálogo. Null = este corte tiene stock propio
	 * (el caso de siempre). Si no es null, este corte se vende con su propio precio pero
	 * el stock que se descuenta es el del corte referenciado acá (ej. "Bife de chorizo"
	 * descontando de "Bife angosto") — un sub-corte que la balanza reconoce por separado
	 * pero que no se desposta por su cuenta. Un solo nivel: el corte referenciado no puede
	 * a su vez tener esto seteado (lo valida CorteService, no la base). */
	@Column(name = "descuenta_stock_de_corte_id")
	private UUID descuentaStockDeCorteId;

	protected CorteEntity() {
	}

	public CorteEntity(String nombre, Integer plu, Cuarto cuarto, TipoProducto tipoProducto, String zonaMapa,
			boolean activo, BigDecimal precioVenta, UUID duenoId, UUID descuentaStockDeCorteId) {
		this.nombre = nombre;
		this.plu = plu;
		this.cuarto = cuarto;
		this.tipoProducto = tipoProducto;
		this.zonaMapa = zonaMapa;
		this.activo = activo;
		this.precioVenta = precioVenta;
		this.duenoId = duenoId;
		this.descuentaStockDeCorteId = descuentaStockDeCorteId;
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

	public TipoProducto getTipoProducto() {
		return tipoProducto;
	}

	public void setTipoProducto(TipoProducto tipoProducto) {
		this.tipoProducto = tipoProducto;
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

	public UUID getDescuentaStockDeCorteId() {
		return descuentaStockDeCorteId;
	}

	public void setDescuentaStockDeCorteId(UUID descuentaStockDeCorteId) {
		this.descuentaStockDeCorteId = descuentaStockDeCorteId;
	}
}
