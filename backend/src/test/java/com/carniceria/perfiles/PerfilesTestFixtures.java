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

	/**
	 * Salteando el flujo de aprobación, para tests que necesitan una cuenta ya operativa.
	 * Si {@code rol} es "dueno", fija {@code dueno_id = id} (V10__multi_negocio.sql: un
	 * dueño es dueño de sí mismo); para cualquier otro rol lo deja en null.
	 */
	@Transactional
	void crearAprobada(UUID id, String nombre, String rol) {
		jdbcTemplate.update(
				"insert into perfiles (id, nombre, rol, estado, dueno_id) "
						+ "values (?, ?, ?, 'aprobado', case when ? = 'dueno' then ? else null end) "
						+ "on conflict (id) do update set rol = excluded.rol, estado = excluded.estado, "
						+ "dueno_id = excluded.dueno_id",
				id, nombre, rol, rol, id);
	}
}
