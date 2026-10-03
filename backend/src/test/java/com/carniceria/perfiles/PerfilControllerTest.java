package com.carniceria.perfiles;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carniceria.shared.security.JwtClaimsHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro con aprobación previa — ver constitution.md, Principio II.
 *
 * Nota de diseño del test: cada escenario usa una sola identidad "actora" de punta a
 * punta. Mezclar identidades distintas dentro de la misma transacción de test (p. ej.
 * Ana se autorregistra y en la misma transacción el dueño la aprueba) rompe, porque el
 * INSERT de Ana queda pendiente de flush en el ORM y se termina ejecutando recién
 * cuando el pedido del dueño ya pisó `request.jwt.claims` con sus propias claims. Por
 * eso las cuentas de prueba se preparan siempre con `PerfilService` + `JwtClaimsHolder`
 * (como en EstimacionControllerTest, Bloque 5), nunca con una llamada HTTP previa de
 * otro usuario.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class PerfilControllerTest {

	private static final String DUENO_TEST_ID = "87b585e4-f4e8-4d9a-858d-efb77058a49d";
	// Usuario real de Supabase Auth (signup), no un UUID inventado: perfiles.id
	// referencia auth.users(id), así que un id que no existe ahí rompe la FK.
	private static final UUID OTRO_USUARIO_TEST_ID = UUID.fromString("8890356c-29b6-473b-8e2d-6f0ff679c994");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtClaimsHolder jwtClaimsHolder;

	@Autowired
	private PerfilesTestFixtures perfilesTestFixtures;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private RequestPostProcessor jwtDeDueno() {
		return jwt().jwt(j -> j.subject(DUENO_TEST_ID).claim("role", "authenticated"));
	}

	private RequestPostProcessor jwtDeRegistroNuevo(UUID id, String nombre, String rolSolicitado) {
		return jwt().jwt(j -> j.subject(id.toString())
				.claim("role", "authenticated")
				.claim("user_metadata", Map.of("nombre", nombre, "rol_solicitado", rolSolicitado)));
	}

	/** Crea una fila `perfiles` pendiente directo en la base, como dueño (ver nota de la clase). */
	private void crearPendienteComoDueno(UUID id, String nombre, String rol) {
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		perfilesTestFixtures.crearPendiente(id, nombre, rol);
		jwtClaimsHolder.clear();
	}

	@Test
	void primerLlamado_creaElPerfilEnEstadoPendiente_sinImportarElRolPedido() throws Exception {
		mockMvc.perform(get("/api/v1/perfiles/yo")
						.with(jwtDeRegistroNuevo(OTRO_USUARIO_TEST_ID, "Juan Pérez", "dueno")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Juan Pérez"))
				.andExpect(jsonPath("$.rol").value("dueno"))
				.andExpect(jsonPath("$.estado").value("pendiente"));
	}

	@Test
	void dueno_veLaListaDePendientesYApruebaUnaCuenta() throws Exception {
		crearPendienteComoDueno(OTRO_USUARIO_TEST_ID, "Ana Gómez", "empleado");

		mockMvc.perform(get("/api/v1/perfiles").with(jwtDeDueno()).param("estado", "pendiente"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id=='" + OTRO_USUARIO_TEST_ID + "')]").exists());

		mockMvc.perform(put("/api/v1/perfiles/{id}", OTRO_USUARIO_TEST_ID).with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("rol", "empleado", "estado", "aprobado"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("aprobado"));
	}

	@Test
	void unaCuentaPendiente_noPuedeAprobarseASiMisma() throws Exception {
		crearPendienteComoDueno(OTRO_USUARIO_TEST_ID, "X", "empleado");

		RequestPostProcessor jwtDePendiente = jwt().jwt(j -> j.subject(OTRO_USUARIO_TEST_ID.toString())
				.claim("role", "authenticated"));

		mockMvc.perform(put("/api/v1/perfiles/{id}", OTRO_USUARIO_TEST_ID).with(jwtDePendiente)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("rol", "empleado", "estado", "aprobado"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
	}
}
