package com.carniceria.perfiles;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solo para tests: inserta una fila `perfiles` pendiente. Tiene que ser un método
 * @Transactional real (no un jdbcTemplate.update suelto en el test) para que
 * RlsSessionAspect propague las claims — ver la nota en PerfilControllerTest.
 */
@Component
class PerfilesTestFixtures {

	private final JdbcTemplate jdbcTemplate;

	PerfilesTestFixtures(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	void crearPendiente(UUID id, String nombre, String rol) {
		jdbcTemplate.update(
				"insert into perfiles (id, nombre, rol, estado) values (?, ?, ?, 'pendiente')",
				id, nombre, rol);
	}
}
