package com.carniceria.escaneo.service;

import com.carniceria.cortes.entity.CorteEntity;
import com.carniceria.cortes.repository.CorteRepository;
import com.carniceria.escaneo.dto.StockCorteResponse;
import com.carniceria.escaneo.entity.StockPorCorteEntity;
import com.carniceria.escaneo.repository.StockRepository;
import com.carniceria.shared.BigDecimals;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

	// FR-208: se marca "Queda poco" por debajo del 15 % de lo despostado para ese corte.
	private static final BigDecimal UMBRAL_QUEDA_POCO = new BigDecimal("0.15");

	private final StockRepository stockRepository;
	private final CorteRepository corteRepository;

	public StockService(StockRepository stockRepository, CorteRepository corteRepository) {
		this.stockRepository = stockRepository;
		this.corteRepository = corteRepository;
	}

	@Transactional
	public List<StockCorteResponse> listar() {
		return stockRepository.findAll().stream().map(this::aRespuesta).toList();
	}

	private StockCorteResponse aRespuesta(StockPorCorteEntity fila) {
		String nombre = corteRepository.findById(fila.getCorteId()).map(CorteEntity::getNombre).orElse(null);
		return new StockCorteResponse(
				fila.getCorteId(), nombre,
				BigDecimals.aTexto(fila.getEntradoKg()),
				BigDecimals.aTexto(fila.getVendidoKg()),
				BigDecimals.aTexto(fila.getStockKg()),
				quedaPoco(fila));
	}

	private boolean quedaPoco(StockPorCorteEntity fila) {
		// Un corte que nunca se despostó (entradoKg = 0) no "queda poco": no hay nada de qué
		// quedarse sin stock todavía.
		if (fila.getEntradoKg().signum() <= 0) {
			return false;
		}
		BigDecimal umbralKg = fila.getEntradoKg().multiply(UMBRAL_QUEDA_POCO);
		return fila.getStockKg().compareTo(umbralKg) < 0;
	}
}
