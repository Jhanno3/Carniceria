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
		dueno, empleado
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
}
