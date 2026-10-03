package com.carniceria.shared.security;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service mínimo de test: existe solo para que RlsPropagationIT pueda ejercitar
 * RlsSessionAspect sobre un método @Transactional real, sin depender de las entidades
 * de negocio que todavía no existen (llegan en el Bloque 4/5).
 */
@Component
class ConteoMediasResesTestService {

	private final JdbcTemplate jdbcTemplate;

	ConteoMediasResesTestService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	long contarMediasReses() {
		Long cantidad = jdbcTemplate.queryForObject("select count(*) from medias_reses", Long.class);
		return cantidad == null ? 0 : cantidad;
	}

	@Transactional
	void insertarMediaResDePrueba(UUID creadoPor) {
		jdbcTemplate.update(
				"insert into medias_reses (peso_kg, precio_kg, creado_por) values (?, ?, ?)",
				new BigDecimal("100.000"), new BigDecimal("5200.00"), creadoPor);
	}
}
