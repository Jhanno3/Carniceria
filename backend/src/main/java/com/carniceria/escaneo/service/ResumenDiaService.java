package com.carniceria.escaneo.service;

import com.carniceria.despostado.repository.MediaResRepository;
import com.carniceria.escaneo.dto.ResumenDiaResponse;
import com.carniceria.escaneo.entity.StockPorCorteEntity;
import com.carniceria.escaneo.entity.VentaEntity;
import com.carniceria.escaneo.repository.StockRepository;
import com.carniceria.escaneo.repository.VentaRepository;
import com.carniceria.shared.BigDecimals;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ver contracts/control-diario-api.md, "GET /control-diario/resumen" (FR-206). */
@Service
public class ResumenDiaService {

	// constitution.md, Principio V: toda fecha de este negocio es en hora Argentina.
	private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

	private final VentaRepository ventaRepository;
	private final MediaResRepository mediaResRepository;
	private final StockRepository stockRepository;

	public ResumenDiaService(VentaRepository ventaRepository, MediaResRepository mediaResRepository,
			StockRepository stockRepository) {
		this.ventaRepository = ventaRepository;
		this.mediaResRepository = mediaResRepository;
		this.stockRepository = stockRepository;
	}

	@Transactional
	public ResumenDiaResponse calcular(LocalDate fecha) {
		Instant desde = fecha.atStartOfDay(ZONA_ARGENTINA).toInstant();
		Instant hasta = fecha.plusDays(1).atStartOfDay(ZONA_ARGENTINA).toInstant().minusNanos(1);

		List<VentaEntity> ventasDelDia = ventaRepository.findByFechaHoraBetweenOrderByFechaHoraDesc(desde, hasta);
		// kgVendidosHoy es neto (plan-fase3.md 3.8): excluye anuladas, para no contradecir a
		// stockVendibleTotal (la vista stock_por_corte ya las excluye desde Fase 2).
		// etiquetasEscaneadasHoy, más abajo, sigue contando todos los escaneos del día — es
		// actividad del mostrador, no ventas netas.
		BigDecimal kgVendidosHoy = ventasDelDia.stream()
				.filter(venta -> !venta.isAnulada())
				.map(VentaEntity::getKg)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		int entradasHoy = mediaResRepository.findByCreadoEnBetweenOrderByCreadoEnDesc(desde, hasta).size();

		// Acumulado histórico, no acotado al día (FR-206): "cuánto queda en cámara ahora".
		BigDecimal stockVendibleTotal = stockRepository.findAll().stream()
				.map(StockPorCorteEntity::getStockKg)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return new ResumenDiaResponse(
				BigDecimals.aTexto(kgVendidosHoy), ventasDelDia.size(),
				BigDecimals.aTexto(stockVendibleTotal), entradasHoy);
	}
}
