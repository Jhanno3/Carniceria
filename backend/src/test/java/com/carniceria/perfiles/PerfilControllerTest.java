package com.carniceria.perfiles;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carniceria.cortes.entity.CorteEntity;
import com.carniceria.cortes.repository.CorteRepository;
import com.carniceria.shared.NegocioTestFixtures;
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

	@Autowired
	private NegocioTestFixtures negocioTestFixtures;

	@Autowired
	private CorteRepository corteRepository;

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
	void unDueno_noPuedeGestionarCuentas_soloAdmin() throws Exception {
		// V9__rol_admin.sql: "dueno" sigue teniendo acceso operativo (cortes, medias_reses,
		// despostado) via is_dueno(), pero gestionar perfiles pasa a ser exclusivo de "admin".
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		perfilesTestFixtures.crearAprobada(OTRO_USUARIO_TEST_ID, "Socio sin Usuarios", "dueno");
		jwtClaimsHolder.clear();

		RequestPostProcessor jwtDeDuenoNoAdmin = jwt().jwt(j -> j.subject(OTRO_USUARIO_TEST_ID.toString())
				.claim("role", "authenticated"));

		mockMvc.perform(get("/api/v1/perfiles").with(jwtDeDuenoNoAdmin).param("estado", "pendiente"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0)); // RLS no le muestra ninguna fila ajena, no es un 403 acá.

		// Se apunta a sí mismo: el guardia universal de auto-modificación (FR-406) lo
		// rechaza antes de llegar a is_admin() — mismo código que protege al admin.
		mockMvc.perform(put("/api/v1/perfiles/{id}", OTRO_USUARIO_TEST_ID).with(jwtDeDuenoNoAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("rol", "admin", "estado", "aprobado"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("NO_PUEDE_MODIFICAR_SU_PROPIA_CUENTA"));
	}

	@Test
	void unDuenoNoAdmin_noPuedeModificarOtraCuenta_devuelve404() throws Exception {
		// A diferencia del test anterior, acá SÍ apunta a otra cuenta (no a sí mismo): RLS
		// le esconde la fila ajena por completo (perfiles_select_propio no se la muestra,
		// y is_admin() tampoco lo deja escribir), así que el UPDATE afecta 0 filas y
		// `existsById` también da false desde su propio punto de vista — 404, no 403 (no
		// revela más de lo que ya podía ver, mismo criterio que PerfilService.actualizar).
		UUID otraCuentaId = UUID.fromString("04d97faa-fd1c-42ce-9fa6-52c697733687");
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		perfilesTestFixtures.crearAprobada(OTRO_USUARIO_TEST_ID, "Socio sin Usuarios", "dueno");
		perfilesTestFixtures.crearAprobada(otraCuentaId, "Otra cuenta", "empleado");
		jwtClaimsHolder.clear();

		RequestPostProcessor jwtDeDuenoNoAdmin = jwt().jwt(j -> j.subject(OTRO_USUARIO_TEST_ID.toString())
				.claim("role", "authenticated"));

		mockMvc.perform(put("/api/v1/perfiles/{id}", otraCuentaId).with(jwtDeDuenoNoAdmin)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("rol", "empleado", "estado", "aprobado"))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("PERFIL_NO_ENCONTRADO"));
	}

	@Test
	void adminVeTodasLasCuentas_sinFiltrarPorEstado() throws Exception {
		// Dos identidades reales de Supabase Auth, reutilizadas como "flexibles" en el
		// resto de la suite (ver VentaControllerTest/ResumenDiaControllerTest, etc.).
		UUID aprobadaId = UUID.fromString("04d97faa-fd1c-42ce-9fa6-52c697733687");
		UUID rechazadaId = UUID.fromString("94b9f75e-e6b7-45d9-8408-5734cd1ae535");

		crearPendienteComoDueno(OTRO_USUARIO_TEST_ID, "Pendiente de prueba", "empleado");
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		perfilesTestFixtures.crearAprobada(aprobadaId, "Aprobada de prueba", "dueno");
		perfilesTestFixtures.crearAprobada(rechazadaId, "Para rechazar", "empleado");
		jwtClaimsHolder.clear();

		mockMvc.perform(put("/api/v1/perfiles/{id}", rechazadaId).with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("rol", "empleado", "estado", "rechazado"))))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/perfiles").with(jwtDeDueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id=='" + OTRO_USUARIO_TEST_ID + "')]").exists())
				.andExpect(jsonPath("$[?(@.id=='" + aprobadaId + "')]").exists())
				.andExpect(jsonPath("$[?(@.id=='" + rechazadaId + "')]").exists());

		// Sin estado, sigue sin ser el listado completo para un no-admin (RLS, no un
		// parámetro que se pueda saltear): ve únicamente su propia fila
		// (perfiles_select_propio), nunca las de los demás vía perfiles_admin_todo.
		RequestPostProcessor jwtDePendiente = jwt().jwt(
				j -> j.subject(OTRO_USUARIO_TEST_ID.toString()).claim("role", "authenticated"));
		mockMvc.perform(get("/api/v1/perfiles").with(jwtDePendiente))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(OTRO_USUARIO_TEST_ID.toString()));
	}

	@Test
	void unaCuentaPendiente_noPuedeAprobarseASiMisma() throws Exception {
		crearPendienteComoDueno(OTRO_USUARIO_TEST_ID, "X", "empleado");

		RequestPostProcessor jwtDePendiente = jwt().jwt(j -> j.subject(OTRO_USUARIO_TEST_ID.toString())
				.claim("role", "authenticated"));

		// Mismo guardia universal de auto-modificación que protege al admin (FR-406): acá
		// también aplica, y de hecho describe mejor este caso que el genérico de antes.
		mockMvc.perform(put("/api/v1/perfiles/{id}", OTRO_USUARIO_TEST_ID).with(jwtDePendiente)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("rol", "empleado", "estado", "aprobado"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("NO_PUEDE_MODIFICAR_SU_PROPIA_CUENTA"));
	}

	@Test
	void unAdmin_noPuedeModificarSuPropiaCuenta() throws Exception {
		mockMvc.perform(put("/api/v1/perfiles/{id}", DUENO_TEST_ID).with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("rol", "admin", "estado", "pausado"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("NO_PUEDE_MODIFICAR_SU_PROPIA_CUENTA"));
	}

	@Test
	void pausarUnEmpleado_leSacaElAccesoDeVerdad_yReactivarLoDevuelve() throws Exception {
		UUID negocioDuenoId = UUID.fromString("04d97faa-fd1c-42ce-9fa6-52c697733687");
		UUID empleadoId = OTRO_USUARIO_TEST_ID;

		jwtClaimsHolder.set("{\"sub\":\"" + negocioDuenoId + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(negocioDuenoId, "Dueño de PerfilControllerTest (pausado)");
		UUID corteId = crearCorteDirecto(negocioDuenoId);
		jwtClaimsHolder.clear();

		jwtClaimsHolder.set("{\"sub\":\"" + empleadoId + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoEmpleado(empleadoId, "Empleado de PerfilControllerTest", negocioDuenoId);
		jwtClaimsHolder.clear();

		RequestPostProcessor jwtDelEmpleado = jwt().jwt(
				j -> j.subject(empleadoId.toString()).claim("role", "authenticated"));

		// Antes de pausar: ve el corte de su propio negocio.
		mockMvc.perform(get("/api/v1/cortes").with(jwtDelEmpleado))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id=='" + corteId + "')]").exists());

		mockMvc.perform(put("/api/v1/perfiles/{id}", empleadoId).with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("rol", "empleado", "estado", "pausado"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.estado").value("pausado"));

		// Pausado: mi_negocio_id() ya no resuelve ningún negocio para él (V20), así que
		// RLS no le deja ver nada, aunque su dueno_id en la columna siga intacto.
		mockMvc.perform(get("/api/v1/cortes").with(jwtDelEmpleado))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(put("/api/v1/perfiles/{id}", empleadoId).with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("rol", "empleado", "estado", "aprobado"))))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/cortes").with(jwtDelEmpleado))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id=='" + corteId + "')]").exists());
	}

	@Test
	void pausarUnDueno_leSacaIsDuenoSobreSuPropioNegocio() throws Exception {
		UUID duenoPausadoId = UUID.fromString("94b9f75e-e6b7-45d9-8408-5734cd1ae535");

		jwtClaimsHolder.set("{\"sub\":\"" + duenoPausadoId + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(duenoPausadoId, "Dueño a pausar");
		UUID corteId = crearCorteDirecto(duenoPausadoId);
		jwtClaimsHolder.clear();

		RequestPostProcessor jwtDelDuenoPausado = jwt().jwt(
				j -> j.subject(duenoPausadoId.toString()).claim("role", "authenticated"));

		mockMvc.perform(get("/api/v1/cortes").with(jwtDelDuenoPausado))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id=='" + corteId + "')]").exists());

		mockMvc.perform(put("/api/v1/perfiles/{id}", duenoPausadoId).with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("rol", "dueno", "estado", "pausado"))))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/cortes").with(jwtDelDuenoPausado))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	private UUID crearCorteDirecto(UUID duenoId) {
		// PLU alto a propósito: "94b9f75e..." es la cuenta real de Facundo, que ya opera
		// su propio negocio de prueba con el catálogo de 17 cortes sembrado (PLU 1-21) —
		// un PLU bajo colisionaría con "cortes_dueno_id_plu_key".
		int plu = java.util.concurrent.ThreadLocalRandom.current().nextInt(100_000, 999_999);
		var corte = new CorteEntity("Corte de prueba", plu, CorteEntity.Cuarto.Ambos, null, true, duenoId);
		corte = corteRepository.save(corte);
		corteRepository.flush();
		return corte.getId();
	}
}
