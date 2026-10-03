package com.carniceria.cortes.controller;

import com.carniceria.cortes.dto.CorteRequest;
import com.carniceria.cortes.dto.CorteResponse;
import com.carniceria.cortes.service.CorteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/cortes")
public class CorteController {

	private final CorteService corteService;

	public CorteController(CorteService corteService) {
		this.corteService = corteService;
	}

	@GetMapping
	public List<CorteResponse> listar(@RequestParam(defaultValue = "false") boolean incluirInactivos) {
		// incluirInactivos=true solo tiene efecto real para un dueño: RLS igual
		// filtra las filas inactivas si quien pregunta es empleado (contracts/despostado-api.md).
		return corteService.listar(incluirInactivos);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CorteResponse crear(@Valid @RequestBody CorteRequest request) {
		return corteService.crear(request);
	}

	@PutMapping("/{id}")
	public CorteResponse actualizar(@PathVariable UUID id, @Valid @RequestBody CorteRequest request) {
		return corteService.actualizar(id, request);
	}
}
