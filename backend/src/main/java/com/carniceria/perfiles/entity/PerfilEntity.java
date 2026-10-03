package com.carniceria.perfiles.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "perfiles")
public class PerfilEntity {

	// En minúscula a propósito: EnumType.STRING guarda name() tal cual, debe coincidir
	// con el check constraint de V1__perfiles.sql / V7__perfiles_estado.sql.
	public enum Rol {
		admin, dueno, empleado
	}

	public enum Estado {
		pendiente, aprobado, rechazado
	}

	@Id
	private UUID id;

	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Rol rol;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Estado estado;

	/**
	 * El "negocio" al que pertenece esta cuenta (V10__multi_negocio.sql). Para un
	 * {@code dueno}, es su propio {@code id} (es dueño de sí mismo); para un
	 * {@code empleado}, es el {@code id} del dueño que lo invitó; {@code null} para
	 * {@code admin} (no opera ningún negocio) o mientras una cuenta sigue pendiente.
	 */
	@Column(name = "dueno_id")
	private UUID duenoId;

	protected PerfilEntity() {
	}

	public PerfilEntity(UUID id, String nombre, Rol rol, Estado estado) {
		this.id = id;
		this.nombre = nombre;
		this.rol = rol;
		this.estado = estado;
	}

	public UUID getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public Rol getRol() {
		return rol;
	}

	public void setRol(Rol rol) {
		this.rol = rol;
	}

	public Estado getEstado() {
		return estado;
	}

	public void setEstado(Estado estado) {
		this.estado = estado;
	}

	public UUID getDuenoId() {
		return duenoId;
	}

	public void setDuenoId(UUID duenoId) {
		this.duenoId = duenoId;
	}
}
