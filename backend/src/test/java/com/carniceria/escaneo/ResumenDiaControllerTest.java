package com.carniceria.escaneo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carniceria.cortes.repository.CorteRepository;
import com.carniceria.cortes.service.CatalogoInicialService;
import com.carniceria.escaneo.service.ConfigEtiquetaInicialService;
import com.carniceria.shared.NegocioTestFixtures;
import com.carniceria.shared.security.JwtClaimsHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Ver contracts/control-diario-api.md, "GET /control-diario/resumen" (FR-206). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class ResumenDiaControllerTest {

	private static final UUID DUENO_TEST_ID = UUID.fromString("04d97faa-fd1c-42ce-9fa6-52c697733687");
	private static final String CODIGO_VACIO = "2000012012501"; // PLU 12, 1,250 kg

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

	@Autowired
	private CorteRepository corteRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private UUID corteVacioId;

	@BeforeEach
	void prepararNegocio() {
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(DUENO_TEST_ID, "Dueño de ResumenDiaControllerTest");
		catalogoInicialService.sembrarSiHaceFalta(DUENO_TEST_ID);
		configEtiquetaInicialService.sembrarSiHaceFalta(DUENO_TEST_ID);
		corteVacioId = corteRepository.findByPlu(12).orElseThrow().getId();
		jwtClaimsHolder.clear();
	}

	private RequestPostProcessor jwtDeDueno() {
		return jwt().jwt(j -> j.subject(DUENO_TEST_ID.toString()).claim("role", "authenticated"));
	}

	@Test
	void sinFecha_usaHoyYCuentaVentasYEntradasDelDia() throws Exception {
		Map<String, Object> despostado = new HashMap<>();
		despostado.put("proveedor", null);
		despostado.put("pesoKg", "10.000");
		despostado.put("precioKg", null);
		despostado.put("cortes", List.of(Map.of("corteId", corteVacioId.toString(), "kg", "10.000")));
		despostado.put("perdidas", null);
		mockMvc.perform(post("/api/v1/medias-reses").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(despostado)))
				.andExpect(status().isCreated());

		for (int i = 0; i < 2; i++) {
			Map<String, Object> escaneo = Map.of("codigo", CODIGO_VACIO, "idClienteLocal", UUID.randomUUID().toString());
			mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(escaneo)))
					.andExpect(status().isCreated());
		}

		mockMvc.perform(get("/api/v1/control-diario/resumen").with(jwtDeDueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.kgVendidosHoy").value("2.500"))
				.andExpect(jsonPath("$.etiquetasEscaneadasHoy").value(2))
				.andExpect(jsonPath("$.stockVendibleTotal").value("7.500"))
				.andExpect(jsonPath("$.entradasHoy").value(1));
	}

	@Test
	void ventaAnulada_noCuentaParaKgVendidosHoyPeroSiParaEtiquetasEscaneadasHoy() throws Exception {
		Map<String, Object> primerEscaneo = Map.of("codigo", CODIGO_VACIO, "idClienteLocal", UUID.randomUUID().toString());
		String respuesta = mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(primerEscaneo)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String ventaAAnularId = objectMapper.readTree(respuesta).get("id").asText();

		Map<String, Object> segundoEscaneo = Map.of("codigo", CODIGO_VACIO, "idClienteLocal", UUID.randomUUID().toString());
		mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(segundoEscaneo)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/ventas/" + ventaAAnularId + "/anular").with(jwtDeDueno()))
				.andExpect(status().isOk());

		// FR-206, plan-fase3.md 3.8: kgVendidosHoy es neto (excluye la anulada, 1,250 kg de
		// las 2,500 escaneadas), etiquetasEscaneadasHoy sigue contando los 2 escaneos —
		// es actividad del mostrador, no ventas netas.
		mockMvc.perform(get("/api/v1/control-diario/resumen").with(jwtDeDueno()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.kgVendidosHoy").value("1.250"))
				.andExpect(jsonPath("$.etiquetasEscaneadasHoy").value(2));
	}

	@Test
	void conFechaSinActividad_devuelveTodoEnCero() throws Exception {
		mockMvc.perform(get("/api/v1/control-diario/resumen").with(jwtDeDueno())
						.param("fecha", LocalDate.now().minusDays(10).toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.kgVendidosHoy").value("0"))
				.andExpect(jsonPath("$.etiquetasEscaneadasHoy").value(0))
				.andExpect(jsonPath("$.entradasHoy").value(0));
	}
}
