package com.carniceria.despostado;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class MediaResControllerTest {

	private static final String DUENO_TEST_ID = "87b585e4-f4e8-4d9a-858d-efb77058a49d";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private UUID corteVacioId;

	@BeforeEach
	void buscarCorteDeEjemplo() {
		// PLU 12 = Vacío, seedeado en V3__seed_cortes.sql.
		corteVacioId = jdbcTemplate.queryForObject(
				"select id from cortes where plu = 12", UUID.class);
	}

	private RequestPostProcessor jwtDeDueno() {
		return jwt().jwt(j -> j.subject(DUENO_TEST_ID).claim("role", "authenticated"));
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

		Long cantidad = jdbcTemplate.queryForObject("select count(*) from medias_reses", Long.class);
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
