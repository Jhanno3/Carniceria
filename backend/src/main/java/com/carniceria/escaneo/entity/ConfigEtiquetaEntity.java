package com.carniceria.escaneo.entity;

import com.carniceria.escaneo.modelo.ConfigEtiqueta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Una fila por negocio (PK {@code dueno_id}, no un id propio) — ver data-model-fase2.md. */
@Entity
@Table(name = "config_etiqueta")
public class ConfigEtiquetaEntity {

	@Id
	@Column(name = "dueno_id")
	private UUID duenoId;

	@Column(name = "prefijo_desde", nullable = false)
	private int prefijoDesde;

	@Column(name = "prefijo_hasta", nullable = false)
	private int prefijoHasta;

	@Column(name = "inicio_plu", nullable = false)
	private int inicioPlu;

	@Column(name = "largo_plu", nullable = false)
	private int largoPlu;

	@Column(name = "inicio_valor", nullable = false)
	private int inicioValor;

	@Column(name = "largo_valor", nullable = false)
	private int largoValor;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_valor", nullable = false)
	private ConfigEtiqueta.TipoValor tipoValor;

	@Column(nullable = false)
	private int decimales;

	protected ConfigEtiquetaEntity() {
	}

	public ConfigEtiquetaEntity(UUID duenoId, int prefijoDesde, int prefijoHasta, int inicioPlu, int largoPlu,
			int inicioValor, int largoValor, ConfigEtiqueta.TipoValor tipoValor, int decimales) {
		this.duenoId = duenoId;
		this.prefijoDesde = prefijoDesde;
		this.prefijoHasta = prefijoHasta;
		this.inicioPlu = inicioPlu;
		this.largoPlu = largoPlu;
		this.inicioValor = inicioValor;
		this.largoValor = largoValor;
		this.tipoValor = tipoValor;
		this.decimales = decimales;
	}

	public UUID getDuenoId() {
		return duenoId;
	}

	public int getPrefijoDesde() {
		return prefijoDesde;
	}

	public void setPrefijoDesde(int prefijoDesde) {
		this.prefijoDesde = prefijoDesde;
	}

	public int getPrefijoHasta() {
		return prefijoHasta;
	}

	public void setPrefijoHasta(int prefijoHasta) {
		this.prefijoHasta = prefijoHasta;
	}

	public int getInicioPlu() {
		return inicioPlu;
	}

	public void setInicioPlu(int inicioPlu) {
		this.inicioPlu = inicioPlu;
	}

	public int getLargoPlu() {
		return largoPlu;
	}

	public void setLargoPlu(int largoPlu) {
		this.largoPlu = largoPlu;
	}

	public int getInicioValor() {
		return inicioValor;
	}

	public void setInicioValor(int inicioValor) {
		this.inicioValor = inicioValor;
	}

	public int getLargoValor() {
		return largoValor;
	}

	public void setLargoValor(int largoValor) {
		this.largoValor = largoValor;
	}

	public ConfigEtiqueta.TipoValor getTipoValor() {
		return tipoValor;
	}

	public void setTipoValor(ConfigEtiqueta.TipoValor tipoValor) {
		this.tipoValor = tipoValor;
	}

	public int getDecimales() {
		return decimales;
	}

	public void setDecimales(int decimales) {
		this.decimales = decimales;
	}

	public ConfigEtiqueta aDominio() {
		return new ConfigEtiqueta(prefijoDesde, prefijoHasta, inicioPlu, largoPlu, inicioValor, largoValor,
				tipoValor, decimales);
	}
}
