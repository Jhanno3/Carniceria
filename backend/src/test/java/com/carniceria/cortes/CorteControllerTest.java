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
	void actualizarCorte_conPrecioVenta_loPersisteYLoDevuelveEnLaRespuesta() throws Exception {
		String id = crearCorteDePrueba(9004);

		Map<String, Object> edicion = new java.util.HashMap<>();
		edicion.put("nombre", "Con precio");
		edicion.put("plu", 9004);
		edicion.put("cuarto", "Ambos");
		edicion.put("zonaMapa", null);
		edicion.put("precioVenta", "9000.00");

		mockMvc.perform(put("/api/v1/cortes/{id}", id).with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(edicion)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.precioVenta").value("9000.00"));
	}

	@Test
	void actualizarCorte_conPrecioVentaNull_loDejaSinPrecio() throws Exception {
		String id = crearCorteDePrueba(9005);

		Map<String, Object> primeraEdicion = new java.util.HashMap<>();
		primeraEdicion.put("nombre", "Con precio");
		primeraEdicion.put("plu", 9005);
		primeraEdicion.put("cuarto", "Ambos");
		primeraEdicion.put("zonaMapa", null);
		primeraEdicion.put("precioVenta", "9000.00");

		mockMvc.perform(put("/api/v1/cortes/{id}", id).with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(primeraEdicion)))
				.andExpect(status().isOk());

		Map<String, Object> segundaEdicion = new java.util.HashMap<>();
		segundaEdicion.put("nombre", "Sin precio");
		segundaEdicion.put("plu", 9005);
		segundaEdicion.put("cuarto", "Ambos");
		segundaEdicion.put("zonaMapa", null);
		segundaEdicion.put("precioVenta", null);

		mockMvc.perform(put("/api/v1/cortes/{id}", id).with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(segundaEdicion)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.precioVenta").isEmpty());
	}

	@Test
	void actualizarCorte_conPrecioVentaNegativo_devuelve400SinLlegarALaBase() throws Exception {
		String id = crearCorteDePrueba(9006);

		Map<String, Object> edicion = new java.util.HashMap<>();
		edicion.put("nombre", "Precio inválido");
		edicion.put("plu", 9006);
		edicion.put("cuarto", "Ambos");
		edicion.put("zonaMapa", null);
		edicion.put("precioVenta", "-100");

		mockMvc.perform(put("/api/v1/cortes/{id}", id).with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(edicion)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("DATOS_INVALIDOS"));
	}

	@Test
	void empleado_leePrecioVentaDeCadaCorte_noQuedaOculto() throws Exception {
		String id = crearCorteDePrueba(9007);

		Map<String, Object> edicion = new java.util.HashMap<>();
		edicion.put("nombre", "Visible para empleado");
		edicion.put("plu", 9007);
		edicion.put("cuarto", "Ambos");
		edicion.put("zonaMapa", null);
		edicion.put("precioVenta", "6500.00");

		mockMvc.perform(put("/api/v1/cortes/{id}", id).with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(edicion)))
				.andExpect(status().isOk());

		jwtClaimsHolder.set("{\"sub\":\"" + EMPLEADO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoEmpleado(EMPLEADO_TEST_ID, "Empleado de CorteControllerTest", DUENO_TEST_ID);
		jwtClaimsHolder.clear();

		String respuestaListado = mockMvc.perform(
						get("/api/v1/cortes").with(jwtDeEmpleado()).param("incluirInactivos", "true"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		com.fasterxml.jackson.databind.JsonNode corteCreado = java.util.stream.StreamSupport.stream(
						objectMapper.readTree(respuestaListado).spliterator(), false)
				.filter(nodo -> nodo.get("plu").asInt() == 9007)
				.findFirst()
				.orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals("6500.00", corteCreado.get("precioVenta").asText());
	}

	private String crearCorteDePrueba(int plu) throws Exception {
		Map<String, Object> alta = new java.util.HashMap<>();
		alta.put("nombre", "Corte de prueba " + plu);
		alta.put("plu", plu);
		alta.put("cuarto", "Ambos");
		alta.put("zonaMapa", null);

		String respuesta = mockMvc.perform(post("/api/v1/cortes").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(alta)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(respuesta).get("id").asText();
	}

	@Test
	void crearCorte_conTipoProductoCerdoYSinCuarto_loCreaConCuartoNulo() throws Exception {
		Map<String, Object> request = new java.util.HashMap<>();
		request.put("nombre", "Bondiola de prueba");
		request.put("plu", 9008);
		request.put("tipoProducto", "Cerdo");
		request.put("zonaMapa", null);

		mockMvc.perform(post("/api/v1/cortes").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipoProducto").value("Cerdo"))
				.andExpect(jsonPath("$.cuarto").isEmpty());
	}

	@Test
	void crearCorte_conTipoProductoCarneYSinCuarto_loCreaConCuartoNulo() throws Exception {
		Map<String, Object> request = new java.util.HashMap<>();
		request.put("nombre", "Rabo de prueba");
		request.put("plu", 9013);
		request.put("tipoProducto", "Carne");
		request.put("zonaMapa", null);

		mockMvc.perform(post("/api/v1/cortes").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipoProducto").value("Carne"))
				.andExpect(jsonPath("$.cuarto").isEmpty());
	}

	@Test
	void crearCorte_conTipoProductoCerdoYCuarto_devuelve400CuartoNoAplica() throws Exception {
		Map<String, Object> request = new java.util.HashMap<>();
		request.put("nombre", "Bondiola de prueba");
		request.put("plu", 9009);
		request.put("tipoProducto", "Cerdo");
		request.put("cuarto", "Trasero");
		request.put("zonaMapa", null);

		mockMvc.perform(post("/api/v1/cortes").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("CUARTO_NO_APLICA"));
	}

	@Test
	void crearCorte_sinTipoProductoNiCuarto_asumeVacunoYExigeCuarto() throws Exception {
		Map<String, Object> request = new java.util.HashMap<>();
		request.put("nombre", "Sin cuarto");
		request.put("plu", 9010);
		request.put("zonaMapa", null);

		mockMvc.perform(post("/api/v1/cortes").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("CUARTO_INVALIDO"));
	}

	@Test
	void crearCorte_conTipoProductoInvalido_devuelve400() throws Exception {
		Map<String, Object> request = new java.util.HashMap<>();
		request.put("nombre", "Tipo inválido");
		request.put("plu", 9011);
		request.put("tipoProducto", "Elefante");
		request.put("zonaMapa", null);

		mockMvc.perform(post("/api/v1/cortes").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("TIPO_PRODUCTO_INVALIDO"));
	}

	@Test
	void actualizarCorte_cambiaTipoProductoAchurasYLimpiaElCuarto() throws Exception {
		Map<String, Object> alta = new java.util.HashMap<>();
		alta.put("nombre", "Chorizo de prueba");
		alta.put("plu", 9012);
		alta.put("cuarto", "Ambos");
		alta.put("zonaMapa", null);

		String respuestaAlta = mockMvc.perform(post("/api/v1/cortes").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(alta)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String id = objectMapper.readTree(respuestaAlta).get("id").asText();

		Map<String, Object> edicion = new java.util.HashMap<>();
		edicion.put("nombre", "Chorizo de prueba");
		edicion.put("plu", 9012);
		edicion.put("tipoProducto", "AchurasEmbutidos");
		edicion.put("zonaMapa", null);

		mockMvc.perform(put("/api/v1/cortes/{id}", id).with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(edicion)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipoProducto").value("AchurasEmbutidos"))
				.andExpect(jsonPath("$.cuarto").isEmpty());
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
