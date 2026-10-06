package com.carniceria.despostado.modelo;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ver DespostadoRepository.sumarKgYValorVentaPorMediaRes (Fase 5, FR-505): solo los kilos
 * de los cortes que tienen `precioVenta` cargado — ni kgConPrecioVenta ni
 * importeConPrecioVenta suman los kilos de los cortes sin precio (se tratarían como precio
 * 0, lo que arrastraría el promedio para abajo sin sentido).
 */
public record IngresoCortesPorMediaRes(UUID mediaResId, BigDecimal kgConPrecioVenta, BigDecimal importeConPrecioVenta) {
}
