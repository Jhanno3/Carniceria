package com.carniceria.despostado.modelo;

import com.carniceria.despostado.entity.PerdidaEntity;
import java.math.BigDecimal;

/** Una fila histórica de `perdidas`, junto con el `peso_kg` de su `media_res` (join) —
 * misma idea que {@link RegistroHistorico}, pero agrupado por tipo de pérdida en vez de
 * por corte (FR-113, extendido para que "Automático" también prellene Hueso/Grasa/Merma). */
public record RegistroPerdidaHistorico(PerdidaEntity.Tipo tipo, BigDecimal kg, BigDecimal pesoKgEntrada) {
}
