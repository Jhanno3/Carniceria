package com.carniceria.escaneo.controller;

import com.carniceria.escaneo.dto.ResumenDiaResponse;
import com.carniceria.escaneo.service.ResumenDiaService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/control-diario")
public class ResumenDiaController {

	// constitution.md, Principio V: toda fecha de este negocio es en hora Argentina.
	private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

	private final ResumenDiaService resumenDiaService;

	public ResumenDiaController(ResumenDiaService resumenDiaService) {
		this.resumenDiaService = resumenDiaService;
	}

	@GetMapping("/resumen")
	public ResumenDiaResponse resumen(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		return resumenDiaService.calcular(fecha != null ? fecha : LocalDate.now(ZONA_ARGENTINA));
	}
}
