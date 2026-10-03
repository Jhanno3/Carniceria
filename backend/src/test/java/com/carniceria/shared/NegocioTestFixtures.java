package com.carniceria.shared;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solo para tests, compartida entre paquetes (de ahí que sea pública, a diferencia de los
 * demás *TestFixtures del repo): deja a una cuenta real de Supabase Auth en condiciones
 * de operar como dueño de su propio negocio (V10__multi_negocio.sql: dueno_id = su propio
 * id). Hay que llamarla con las claims de ESA MISMA cuenta ya puestas en
 * {@link com.carniceria.shared.security.JwtClaimsHolder} — el INSERT pasa por la política
 * "perfiles_insert_propio" ({@code id = auth.uid()}), no hace falta actuar como admin.
 */
@Component
public class NegocioTestFixtures {

	private final JdbcTemplate jdbcTemplate;

	public NegocioTestFixtures(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	public void registrarComoDueno(UUID id, String nombre) {
		jdbcTemplate.update(
				"insert into perfiles (id, nombre, rol, estado, dueno_id) values (?, ?, 'dueno', 'aprobado', ?) "
						+ "on conflict (id) do update set rol = excluded.rol, estado = excluded.estado, dueno_id = excluded.dueno_id",
				id, nombre, id);
	}

	/** Llamar con las claims del propio {@code id} ya puestas (mismo motivo que arriba). */
	@Transactional
	public void registrarComoEmpleado(UUID id, String nombre, UUID duenoId) {
		jdbcTemplate.update(
				"insert into perfiles (id, nombre, rol, estado, dueno_id) values (?, ?, 'empleado', 'aprobado', ?) "
						+ "on conflict (id) do update set rol = excluded.rol, estado = excluded.estado, dueno_id = excluded.dueno_id",
				id, nombre, duenoId);
	}
}
