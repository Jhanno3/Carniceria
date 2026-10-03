package com.carniceria.escaneo.controller;

import com.carniceria.escaneo.dto.StockCorteResponse;
import com.carniceria.escaneo.service.StockService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock")
public class StockController {

	private final StockService stockService;

	public StockController(StockService stockService) {
		this.stockService = stockService;
	}

	@GetMapping
	public List<StockCorteResponse> listar() {
		return stockService.listar();
	}
}
