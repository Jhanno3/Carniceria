package com.carniceria.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

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
 * El usuario de prueba (DUENO_TEST_ID) ya existe en auth.users y en perfiles con
 * rol='dueno' (creado a mano al validar el Bloque 2 — ver research.md).
 */
@SpringBootTest
@Transactional
@Rollback
class RlsPropagationIT {

	private static final UUID DUENO_TEST_ID = UUID.fromString("87b585e4-f4e8-4d9a-858d-efb77058a49d");

	@Autowired
	private ConteoMediasResesTestService conteoMediasResesTestService;

	@Autowired
	private JwtClaimsHolder jwtClaimsHolder;

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

		conteoMediasResesTestService.insertarMediaResDePrueba(DUENO_TEST_ID);
		long cantidad = conteoMediasResesTestService.contarMediasReses();

		assertThat(cantidad).isEqualTo(1);
	}
}
