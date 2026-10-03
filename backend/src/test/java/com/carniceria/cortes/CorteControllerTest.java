package com.carniceria.cortes;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class CorteControllerTest {

	private static final String DUENO_TEST_ID = "87b585e4-f4e8-4d9a-858d-efb77058a49d";

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private RequestPostProcessor jwtDeDueno() {
		return jwt().jwt(j -> j.subject(DUENO_TEST_ID).claim("role", "authenticated"));
	}

	@Test
	void listarCortes_devuelveLosCortesSeedeados() throws Exception {
		mockMvc.perform(get("/api/v1/cortes").with(jwtDeDueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(21)));
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
		request.put("plu", 12); // ya existe: Vacío (seed de V3__seed_cortes.sql)
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
}
