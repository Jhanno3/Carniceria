package com.carniceria.escaneo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carniceria.cortes.service.CatalogoInicialService;
import com.carniceria.escaneo.service.ConfigEtiquetaInicialService;
import com.carniceria.shared.NegocioTestFixtures;
import com.carniceria.shared.security.JwtClaimsHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Ver contracts/control-diario-api.md, "GET/PUT /config-etiqueta" (FR-209). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class ConfigEtiquetaControllerTest {

	private static final UUID DUENO_TEST_ID = UUID.fromString("04d97faa-fd1c-42ce-9fa6-52c697733687");
	private static final UUID EMPLEADO_TEST_ID = UUID.fromString("8890356c-29b6-473b-8e2d-6f0ff679c994");
	private static final UUID OTRO_DUENO_TEST_ID = UUID.fromString("87b585e4-f4e8-4d9a-858d-efb77058a49d");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtClaimsHolder jwtClaimsHolder;

	@Autowired
	private NegocioTestFixtures negocioTestFixtures;

	@Autowired
	private CatalogoInicialService catalogoInicialService;

	@Autowired
	private ConfigEtiquetaInicialService configEtiquetaInicialService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void prepararNegocio() {
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(DUENO_TEST_ID, "Dueño de ConfigEtiquetaControllerTest");
		catalogoInicialService.sembrarSiHaceFalta(DUENO_TEST_ID);
		configEtiquetaInicialService.sembrarSiHaceFalta(DUENO_TEST_ID);
		jwtClaimsHolder.clear();
	}

	private RequestPostProcessor jwtDeDueno() {
		return jwt().jwt(j -> j.subject(DUENO_TEST_ID.toString()).claim("role", "authenticated"));
	}

	private RequestPostProcessor jwtDeEmpleado() {
		return jwt().jwt(j -> j.subject(EMPLEADO_TEST_ID.toString()).claim("role", "authenticated"));
	}

	@Test
	void obtener_devuelveLaFilaSembrada() throws Exception {
		mockMvc.perform(get("/api/v1/config-etiqueta").with(jwtDeDueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prefijoDesde").value(20))
				.andExpect(jsonPath("$.prefijoHasta").value(29))
				.andExpect(jsonPath("$.inicioPlu").value(2))
				.andExpect(jsonPath("$.largoPlu").value(5))
				.andExpect(jsonPath("$.inicioValor").value(7))
				.andExpect(jsonPath("$.largoValor").value(5))
				.andExpect(jsonPath("$.tipoValor").value("peso"))
				.andExpect(jsonPath("$.decimales").value(3));
	}

	@Test
	void actualizar_cambiaLaConfigYElGetSiguienteLaRefleja() throws Exception {
		Map<String, Object> edicion = Map.of(
				"prefijoDesde", 21, "prefijoHasta", 28,
				"inicioPlu", 2, "largoPlu", 4,
				"inicioValor", 6, "largoValor", 5,
				"tipoValor", "peso", "decimales", 3);

		mockMvc.perform(put("/api/v1/config-etiqueta").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(edicion)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prefijoDesde").value(21))
				.andExpect(jsonPath("$.largoPlu").value(4));

		mockMvc.perform(get("/api/v1/config-etiqueta").with(jwtDeDueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prefijoDesde").value(21))
				.andExpect(jsonPath("$.inicioValor").value(6));
	}

	@Test
	void actualizar_conPluYValorSuperpuestos_devuelve400() throws Exception {
		// PLU ocupa 2-6, valor arranca en 5: se superponen.
		Map<String, Object> edicion = Map.of(
				"prefijoDesde", 20, "prefijoHasta", 29,
				"inicioPlu", 2, "largoPlu", 5,
				"inicioValor", 5, "largoValor", 5,
				"tipoValor", "peso", "decimales", 3);

		mockMvc.perform(put("/api/v1/config-etiqueta").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(edicion)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("CONFIGURACION_ETIQUETA_INVALIDA"));
	}

	@Test
	void empleadoDelMismoNegocio_noPuedeLeerNiEscribir() throws Exception {
		jwtClaimsHolder.set("{\"sub\":\"" + EMPLEADO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoEmpleado(
				EMPLEADO_TEST_ID, "Empleado de ConfigEtiquetaControllerTest", DUENO_TEST_ID);
		jwtClaimsHolder.clear();

		mockMvc.perform(get("/api/v1/config-etiqueta").with(jwtDeEmpleado()))
				.andExpect(status().is4xxClientError());

		Map<String, Object> edicion = Map.of(
				"prefijoDesde", 20, "prefijoHasta", 29,
				"inicioPlu", 2, "largoPlu", 5,
				"inicioValor", 7, "largoValor", 5,
				"tipoValor", "peso", "decimales", 3);
		mockMvc.perform(put("/api/v1/config-etiqueta").with(jwtDeEmpleado())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(edicion)))
				.andExpect(status().is4xxClientError());
	}

	@Test
	void segundoNegocio_noVeNiPuedePisarLaConfigDelPrimero() throws Exception {
		jwtClaimsHolder.set("{\"sub\":\"" + OTRO_DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(OTRO_DUENO_TEST_ID, "Otro negocio de ConfigEtiquetaControllerTest");
		catalogoInicialService.sembrarSiHaceFalta(OTRO_DUENO_TEST_ID);
		configEtiquetaInicialService.sembrarSiHaceFalta(OTRO_DUENO_TEST_ID);
		jwtClaimsHolder.clear();

		Map<String, Object> edicion = Map.of(
				"prefijoDesde", 25, "prefijoHasta", 26,
				"inicioPlu", 2, "largoPlu", 5,
				"inicioValor", 7, "largoValor", 5,
				"tipoValor", "peso", "decimales", 3);
		RequestPostProcessor jwtDeOtroDueno = jwt().jwt(
				j -> j.subject(OTRO_DUENO_TEST_ID.toString()).claim("role", "authenticated"));
		mockMvc.perform(put("/api/v1/config-etiqueta").with(jwtDeOtroDueno)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(edicion)))
				.andExpect(status().isOk());

		// La del primer negocio sigue en sus valores de ejemplo originales.
		mockMvc.perform(get("/api/v1/config-etiqueta").with(jwtDeDueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.prefijoDesde").value(20))
				.andExpect(jsonPath("$.prefijoHasta").value(29));
	}
}
