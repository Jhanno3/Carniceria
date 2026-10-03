package com.carniceria.cortes;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carniceria.cortes.service.CatalogoInicialService;
import com.carniceria.shared.NegocioTestFixtures;
import com.carniceria.shared.security.JwtClaimsHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * El dueño de este test (DUENO_TEST_ID) es una cuenta real de Supabase Auth sin perfil
 * permanente, no el "87b585e4..." de otros tests — ese pasó a ser la cuenta "admin" única
 * (V9__rol_admin.sql) y un admin no es dueño de ningún negocio (V10__multi_negocio.sql):
 * no vería ningún corte. Cada test se registra como dueño y siembra su propio catálogo
 * en el {@code @BeforeEach}, igual que haría {@code GET /perfiles/yo} en la app real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class CorteControllerTest {

	private static final UUID DUENO_TEST_ID = UUID.fromString("04d97faa-fd1c-42ce-9fa6-52c697733687");
	private static final UUID EMPLEADO_TEST_ID = UUID.fromString("8890356c-29b6-473b-8e2d-6f0ff679c994");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtClaimsHolder jwtClaimsHolder;

	@Autowired
	private NegocioTestFixtures negocioTestFixtures;

	@Autowired
	private CatalogoInicialService catalogoInicialService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void prepararDuenoConCatalogo() {
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(DUENO_TEST_ID, "Dueño de CorteControllerTest");
		catalogoInicialService.sembrarSiHaceFalta(DUENO_TEST_ID);
		jwtClaimsHolder.clear();
	}

	private RequestPostProcessor jwtDeDueno() {
		return jwt().jwt(j -> j.subject(DUENO_TEST_ID.toString()).claim("role", "authenticated"));
	}

	private RequestPostProcessor jwtDeEmpleado() {
		return jwt().jwt(j -> j.subject(EMPLEADO_TEST_ID.toString()).claim("role", "authenticated"));
	}

	@Test
	void listarCortes_devuelveElCatalogoSembradoAlSerAprobado() throws Exception {
		// 21 cortes seedeados originalmente, menos los 4 sacados en V8 (ver
		// V8__eliminar_cortes_redundantes.sql) = 17, sembrados por CatalogoInicialService.
		mockMvc.perform(get("/api/v1/cortes").with(jwtDeDueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(17)));
	}

	@Test
	void crearCorte_conPluNuevo_loCreaYDevuelve201() throws Exception {
		Map<String, Object> request = new java.util.HashMap<>();
		request.put("nombre", "Corte de prueba");
		request.put("plu", 9001);
		request.put("cuarto", "Ambos");
		request.put("zonaMapa", null);

		mockMvc.perform(post("/api/v1/cortes").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.plu").value(9001))
				.andExpect(jsonPath("$.id").exists());
	}

	@Test
	void crearCorte_conPluYaExistente_devuelve409() throws Exception {
		Map<String, Object> request = new java.util.HashMap<>();
		request.put("nombre", "Duplicado");
		request.put("plu", 12); // ya existe: Vacío (sembrado por CatalogoInicialService en @BeforeEach)
		request.put("cuarto", "Trasero");
		request.put("zonaMapa", null);

		mockMvc.perform(post("/api/v1/cortes").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("PLU_DUPLICADO"));
	}

	@Test
	void actualizarCorte_cambiaNombreYActivo() throws Exception {
		Map<String, Object> alta = new java.util.HashMap<>();
		alta.put("nombre", "Para editar");
		alta.put("plu", 9002);
		alta.put("cuarto", "Delantero");
		alta.put("zonaMapa", "cogote");

		String respuestaAlta = mockMvc.perform(post("/api/v1/cortes").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(alta)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		String id = objectMapper.readTree(respuestaAlta).get("id").asText();

		Map<String, Object> edicion = new java.util.HashMap<>();
		edicion.put("nombre", "Editado");
		edicion.put("plu", 9002);
		edicion.put("cuarto", "Delantero");
		edicion.put("zonaMapa", "cogote");
		edicion.put("activo", false);

		mockMvc.perform(put("/api/v1/cortes/{id}", id).with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(edicion)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nombre").value("Editado"))
				.andExpect(jsonPath("$.activo").value(false));
	}

	@Test
	void empleadoDelMismoNegocio_puedeLeerPeroNoCrearCortes() throws Exception {
		// Regresión de V11__fix_dueno_todo_exige_rol.sql: antes de esa migración,
		// "cortes_dueno_todo" solo miraba dueno_id (no el rol), así que un empleado del
		// mismo negocio que su dueño colaba por esa política "for all" y podía escribir.
		jwtClaimsHolder.set("{\"sub\":\"" + EMPLEADO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoEmpleado(EMPLEADO_TEST_ID, "Empleado de CorteControllerTest", DUENO_TEST_ID);
		jwtClaimsHolder.clear();

		mockMvc.perform(get("/api/v1/cortes").with(jwtDeEmpleado()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(17)));

		Map<String, Object> request = new java.util.HashMap<>();
		request.put("nombre", "No debería poder");
		request.put("plu", 9003);
		request.put("cuarto", "Ambos");
		request.put("zonaMapa", null);

		mockMvc.perform(post("/api/v1/cortes").with(jwtDeEmpleado())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
	}
}
