package com.carniceria.reportes.controller;

import com.carniceria.reportes.dto.ReporteCategoriaItem;
import com.carniceria.reportes.dto.ReportePeriodoItem;
import com.carniceria.reportes.dto.ReporteProveedorItem;
import com.carniceria.reportes.service.ReporteService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {

	private final ReporteService reporteService;

	public ReporteController(ReporteService reporteService) {
		this.reporteService = reporteService;
	}

	@GetMapping("/por-proveedor")
	public List<ReporteProveedorItem> porProveedor(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		return reporteService.porProveedor(desde, hasta);
	}

	@GetMapping("/por-categoria")
	public List<ReporteCategoriaItem> porCategoria(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		return reporteService.porCategoria(desde, hasta);
	}

	@GetMapping("/por-periodo")
	public List<ReportePeriodoItem> porPeriodo(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam String periodo) {
		return reporteService.porPeriodo(desde, hasta, periodo);
	}
}
