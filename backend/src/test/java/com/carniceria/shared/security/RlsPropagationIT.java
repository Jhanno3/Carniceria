package com.carniceria.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.carniceria.shared.NegocioTestFixtures;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba, contra la base real de Supabase, que RLS es la autoridad real sobre los
 * datos (constitution.md, Principio II): sin las claims del JWT propagadas, ninguna
 * fila es visible aunque exista un dueño; con las claims de un dueño real, sí.
 *
 * El usuario de prueba (DUENO_TEST_ID) es una cuenta real de Supabase Auth sin perfil
 * permanente — se registra como dueño (V10__multi_negocio.sql) al principio de cada test
 * que lo necesita, vía {@link NegocioTestFixtures}. (No es el mismo usuario "87b585e4..."
 * de otros tests: ese pasó a ser la cuenta "admin" única, ver V9__rol_admin.sql, y un
 * admin no es dueño de ningún negocio — no podría ver las filas que este test inserta.)
 */
@SpringBootTest
@Transactional
@Rollback
class RlsPropagationIT {

	private static final UUID DUENO_TEST_ID = UUID.fromString("04d97faa-fd1c-42ce-9fa6-52c697733687");

	@Autowired
	private ConteoMediasResesTestService conteoMediasResesTestService;

	@Autowired
	private JwtClaimsHolder jwtClaimsHolder;

	@Autowired
	private NegocioTestFixtures negocioTestFixtures;

	@BeforeEach
	@AfterEach
	void limpiarClaims() {
		jwtClaimsHolder.clear();
	}

	@Test
	void sinClaimsPropagadas_noVeNingunaFila() {
		long cantidad = conteoMediasResesTestService.contarMediasReses();

		assertThat(cantidad).isZero();
	}

	@Test
	void conClaimsDeUnDuenoReal_veLasFilasQueInserta() {
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(DUENO_TEST_ID, "Dueño de RlsPropagationIT");

		conteoMediasResesTestService.insertarMediaResDePrueba(DUENO_TEST_ID);
		long cantidad = conteoMediasResesTestService.contarMediasReses();

		assertThat(cantidad).isEqualTo(1);
	}
}
