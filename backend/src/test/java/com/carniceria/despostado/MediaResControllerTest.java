package com.carniceria.despostado;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carniceria.cortes.repository.CorteRepository;
import com.carniceria.cortes.service.CatalogoInicialService;
import com.carniceria.shared.NegocioTestFixtures;
import com.carniceria.shared.security.JwtClaimsHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contra Supabase real. Ver contracts/despostado-api.md, "POST /medias-reses".
 *
 * El dueño de este test (DUENO_TEST_ID) es una cuenta real de Supabase Auth sin perfil
 * permanente, no el "87b585e4..." de otros tests — ese pasó a ser la cuenta "admin" única
 * (V9__rol_admin.sql) y un admin no es dueño de ningún negocio (V10__multi_negocio.sql).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class MediaResControllerTest {

	private static final UUID DUENO_TEST_ID = UUID.fromString("04d97faa-fd1c-42ce-9fa6-52c697733687");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtClaimsHolder jwtClaimsHolder;

	@Autowired
	private NegocioTestFixtures negocioTestFixtures;

	@Autowired
	private CatalogoInicialService catalogoInicialService;

	@Autowired
	private CorteRepository corteRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private UUID corteVacioId;

	@BeforeEach
	void prepararDuenoConCatalogo() {
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(DUENO_TEST_ID, "Dueño de MediaResControllerTest");
		catalogoInicialService.sembrarSiHaceFalta(DUENO_TEST_ID);
		// PLU 12 = Vacío, sembrado por CatalogoInicialService. Por JPA (no jdbcTemplate
		// crudo): un SELECT directo no dispara el auto-flush de Hibernate y no vería los
		// INSERT que sembrarSiHaceFalta todavía tiene sin volcar a la base.
		corteVacioId = corteRepository.findByPlu(12).orElseThrow().getId();
		jwtClaimsHolder.clear();
	}

	private RequestPostProcessor jwtDeDueno() {
		return jwt().jwt(j -> j.subject(DUENO_TEST_ID.toString()).claim("role", "authenticated"));
	}

	@Test
	void cargarEntrada_conDatosValidos_persisteYDevuelveElResumenCalculado() throws Exception {
		Map<String, Object> body = cuerpoDeEjemplo();

		mockMvc.perform(post("/api/v1/medias-reses").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").exists())
				.andExpect(jsonPath("$.resumen.vendibleKg").value("81.000"))
				.andExpect(jsonPath("$.resumen.perdidaKg").value("19.000"))
				.andExpect(jsonPath("$.resumen.costoTotal").value("520000"))
				.andExpect(jsonPath("$.resumen.costoKgVendible").value("6420"));
	}

	@Test
	void cargarEntrada_conCategoriaValida_laDevuelve() throws Exception {
		Map<String, Object> body = cuerpoDeEjemplo();
		body.put("categoria", "Novillo");

		mockMvc.perform(post("/api/v1/medias-reses").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.categoria").value("Novillo"));
	}

	@Test
	void cargarEntrada_sinCategoria_laDevuelveNula() throws Exception {
		mockMvc.perform(post("/api/v1/medias-reses").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(cuerpoDeEjemplo())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.categoria").isEmpty());
	}

	@Test
	void cargarEntrada_conCategoriaInvalida_devuelve400() throws Exception {
		Map<String, Object> body = cuerpoDeEjemplo();
		body.put("categoria", "Elefante");

		mockMvc.perform(post("/api/v1/medias-reses").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("CATEGORIA_INVALIDA"));
	}

	@Test
	void cargarEntrada_conCorteInexistente_devuelve400() throws Exception {
		Map<String, Object> body = cuerpoDeEjemplo();
		body.put("cortes", List.of(Map.of("corteId", UUID.randomUUID().toString(), "kg", "10.000")));

		mockMvc.perform(post("/api/v1/medias-reses").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("CORTE_INEXISTENTE"));
	}

	@Test
	void cargarEntrada_conKgInvalido_noPersisteNada() throws Exception {
		Map<String, Object> body = cuerpoDeEjemplo();
		body.put("cortes", List.of(Map.of("corteId", corteVacioId.toString(), "kg", "-1.000")));

		mockMvc.perform(post("/api/v1/medias-reses").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("KG_INVALIDO"));

		// No un count(*) global: la base de dev es compartida con uso manual real, puede
		// haber filas de otras cargas. Se filtra por el proveedor fijo de este fixture, que
		// ninguna carga real usa.
		Long cantidad = jdbcTemplate.queryForObject(
				"select count(*) from medias_reses where proveedor = ?", Long.class, "Frigorífico de prueba");
		org.assertj.core.api.Assertions.assertThat(cantidad).isZero();
	}

	@Test
	void empleado_noPuedeCargarNiLeerEntradas() throws Exception {
		Map<String, Object> body = cuerpoDeEjemplo();
		RequestPostProcessor jwtDeEmpleado = jwt().jwt(j -> j.subject(UUID.randomUUID().toString())
				.claim("role", "authenticated"));

		// Sin perfil de dueño (ni siquiera perfil de empleado): is_dueno() da false,
		// y como medias_reses no tiene ninguna política para otro rol, RLS bloquea todo.
		mockMvc.perform(post("/api/v1/medias-reses").with(jwtDeEmpleado)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().is4xxClientError());
	}

	private Map<String, Object> cuerpoDeEjemplo() {
		Map<String, Object> body = new HashMap<>();
		body.put("proveedor", "Frigorífico de prueba");
		body.put("pesoKg", "100.000");
		body.put("precioKg", "5200.00");
		body.put("cortes", List.of(Map.of("corteId", corteVacioId.toString(), "kg", "81.000")));
		body.put("perdidas", Map.of("hueso", "11.000", "grasa", "6.000", "merma", "2.000"));
		return body;
	}
}
