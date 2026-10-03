package com.carniceria.despostado.modelo;

import java.math.BigDecimal;
import java.util.UUID;

/** Una fila histórica de `despostado`, junto con el `peso_kg` de su `media_res` (join). */
public record RegistroHistorico(UUID corteId, BigDecimal kg, BigDecimal pesoKgEntrada) {
}
