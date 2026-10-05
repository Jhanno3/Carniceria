package com.carniceria.escaneo.controller;

import com.carniceria.escaneo.dto.ConfigEtiquetaDto;
import com.carniceria.escaneo.service.ConfigEtiquetaService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ver contracts/control-diario-api.md, "GET/PUT /config-etiqueta" (FR-209). Solo dueño. */
@RestController
@RequestMapping("/api/v1/config-etiqueta")
public class ConfigEtiquetaController {

	private final ConfigEtiquetaService configEtiquetaService;

	public ConfigEtiquetaController(ConfigEtiquetaService configEtiquetaService) {
		this.configEtiquetaService = configEtiquetaService;
	}

	@GetMapping
	public ConfigEtiquetaDto obtener(@AuthenticationPrincipal Jwt jwt) {
		return configEtiquetaService.obtener(UUID.fromString(jwt.getSubject()));
	}

	@PutMapping
	public ConfigEtiquetaDto actualizar(@Valid @RequestBody ConfigEtiquetaDto request,
			@AuthenticationPrincipal Jwt jwt) {
		return configEtiquetaService.actualizar(UUID.fromString(jwt.getSubject()), request);
	}
}
