package com.carniceria.despostado.controller;

import com.carniceria.despostado.dto.CargarEntradaRequest;
import com.carniceria.despostado.dto.EstimacionResponse;
import com.carniceria.despostado.dto.MediaResResponse;
import com.carniceria.despostado.service.MediaResService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/medias-reses")
public class MediaResController {

	private final MediaResService mediaResService;

	public MediaResController(MediaResService mediaResService) {
		this.mediaResService = mediaResService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MediaResResponse cargarEntrada(@Valid @RequestBody CargarEntradaRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		return mediaResService.cargarEntrada(request, UUID.fromString(jwt.getSubject()));
	}

	@GetMapping("/estimacion")
	public EstimacionResponse estimar(@RequestParam BigDecimal pesoKg) {
		return mediaResService.estimar(pesoKg);
	}

	@GetMapping("/{id}")
	public MediaResResponse buscarPorId(@PathVariable UUID id) {
		return mediaResService.buscarPorId(id);
	}

	@PutMapping("/{id}")
	public MediaResResponse actualizar(@PathVariable UUID id, @Valid @RequestBody CargarEntradaRequest request) {
		return mediaResService.actualizar(id, request);
	}

	@GetMapping
	public List<MediaResResponse> listar(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		return mediaResService.listar(desde, hasta);
	}
}
