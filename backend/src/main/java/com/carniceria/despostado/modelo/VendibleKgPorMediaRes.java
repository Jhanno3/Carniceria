package com.carniceria.despostado.modelo;

import java.math.BigDecimal;
import java.util.UUID;

/** Ver DespostadoRepository.sumarVendibleKgPorMediaRes — plan-fase4.md, 3.5. */
public record VendibleKgPorMediaRes(UUID mediaResId, BigDecimal vendibleKg) {
}
