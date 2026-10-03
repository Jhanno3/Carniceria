package com.carniceria.perfiles.controller;

import com.carniceria.perfiles.dto.PerfilActualizarRequest;
import com.carniceria.perfiles.dto.PerfilResponse;
import com.carniceria.perfiles.service.PerfilService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/perfiles")
public class PerfilController {

	private final PerfilService perfilService;

	public PerfilController(PerfilService perfilService) {
		this.perfilService = perfilService;
	}

	/** Cualquier usuario autenticado — es cómo el frontend sabe si ya lo aprobaron. */
	@GetMapping("/yo")
	public PerfilResponse obtenerPropio(@AuthenticationPrincipal Jwt jwt) {
		return perfilService.obtenerOCrearPropio(jwt);
	}

	/** Solo admin (RLS): lista de cuentas para la pantalla "Usuarios". */
	@GetMapping
	public List<PerfilResponse> listar(@RequestParam(defaultValue = "pendiente") String estado) {
		return perfilService.listarPorEstado(estado);
	}

	/** Solo admin (RLS): aprobar/rechazar, o cambiar el rol de una cuenta. */
	@PutMapping("/{id}")
	public PerfilResponse actualizar(@PathVariable UUID id, @Valid @RequestBody PerfilActualizarRequest request) {
		return perfilService.actualizar(id, request);
	}
}
