package com.carniceria.escaneo.controller;

import com.carniceria.escaneo.dto.EscanearRequest;
import com.carniceria.escaneo.dto.VentaResponse;
import com.carniceria.escaneo.service.VentaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ventas")
public class VentaController {

	private final VentaService ventaService;

	public VentaController(VentaService ventaService) {
		this.ventaService = ventaService;
	}

	@PostMapping
	public ResponseEntity<VentaResponse> escanear(@Valid @RequestBody EscanearRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		VentaService.ResultadoEscaneo resultado = ventaService.escanear(request, UUID.fromString(jwt.getSubject()));
		HttpStatus status = resultado.yaRegistrada() ? HttpStatus.OK : HttpStatus.CREATED;
		return ResponseEntity.status(status).body(resultado.venta());
	}

	@GetMapping
	public List<VentaResponse> listar(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(required = false) Integer limite) {
		return ventaService.listar(desde, hasta, limite);
	}
}
