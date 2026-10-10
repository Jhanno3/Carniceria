package com.carniceria.escaneo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carniceria.cortes.dto.CorteRequest;
import com.carniceria.cortes.repository.CorteRepository;
import com.carniceria.cortes.service.CatalogoInicialService;
import com.carniceria.cortes.service.CorteService;
import com.carniceria.escaneo.entity.VentaEntity;
import com.carniceria.escaneo.repository.VentaRepository;
import com.carniceria.escaneo.service.ConfigEtiquetaInicialService;
import com.carniceria.shared.NegocioTestFixtures;
import com.carniceria.shared.security.JwtClaimsHolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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

/** Ver contracts/control-diario-api.md, "GET /stock" (FR-205, FR-208). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class StockControllerTest {

	private static final UUID DUENO_TEST_ID = UUID.fromString("04d97faa-fd1c-42ce-9fa6-52c697733687");
	private static final UUID EMPLEADO_TEST_ID = UUID.fromString("8890356c-29b6-473b-8e2d-6f0ff679c994");
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

	@Autowired
	private CorteService corteService;

	@Autowired
	private VentaRepository ventaRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private UUID corteVacioId;
	private UUID corteAsadoId;

	@BeforeEach
	void prepararNegocio() {
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(DUENO_TEST_ID, "Dueño de StockControllerTest");
		catalogoInicialService.sembrarSiHaceFalta(DUENO_TEST_ID);
		configEtiquetaInicialService.sembrarSiHaceFalta(DUENO_TEST_ID);
		corteVacioId = corteRepository.findByPlu(12).orElseThrow().getId();
		corteAsadoId = corteRepository.findByPlu(11).orElseThrow().getId();
		jwtClaimsHolder.clear();
	}

	private RequestPostProcessor jwtDeDueno() {
		return jwt().jwt(j -> j.subject(DUENO_TEST_ID.toString()).claim("role", "authenticated"));
	}

	private RequestPostProcessor jwtDeEmpleado() {
		return jwt().jwt(j -> j.subject(EMPLEADO_TEST_ID.toString()).claim("role", "authenticated"));
	}

	private void cargarDespostado() throws Exception {
		Map<String, Object> body = new HashMap<>();
		body.put("proveedor", null);
		body.put("pesoKg", "20.000");
		body.put("precioKg", null);
		body.put("cortes", List.of(
				Map.of("corteId", corteVacioId.toString(), "kg", "10.000"),
				Map.of("corteId", corteAsadoId.toString(), "kg", "5.000")));
		body.put("perdidas", null);

		mockMvc.perform(post("/api/v1/medias-reses").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isCreated());
	}

	private void escanearVacio(int veces) throws Exception {
		for (int i = 0; i < veces; i++) {
			Map<String, Object> body = Map.of("codigo", CODIGO_VACIO, "idClienteLocal", UUID.randomUUID().toString());
			mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(body)))
					.andExpect(status().isCreated());
		}
	}

	private JsonNode filaDe(JsonNode stock, String corteNombre) {
		for (JsonNode fila : stock) {
			if (fila.get("corteNombre").asText().equals(corteNombre)) {
				return fila;
			}
		}
		throw new AssertionError("No se encontró el corte " + corteNombre + " en " + stock);
	}

	@Test
	void stock_calculaEntradoVendidoYQuedaPocoCorrectamente() throws Exception {
		cargarDespostado();
		escanearVacio(7); // 7 × 1,250 = 8,750 vendidos de 10 → stock 1,250 (12,5 % < 15 %)

		String respuesta = mockMvc.perform(get("/api/v1/stock").with(jwtDeDueno()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode stock = objectMapper.readTree(respuesta);

		JsonNode vacio = filaDe(stock, "Vacío");
		assertThat(vacio.get("entradoKg").asText()).isEqualTo("10.000");
		assertThat(vacio.get("vendidoKg").asText()).isEqualTo("8.750");
		assertThat(vacio.get("stockKg").asText()).isEqualTo("1.250");
		assertThat(vacio.get("quedaPoco").asBoolean()).isTrue();

		JsonNode asado = filaDe(stock, "Asado");
		assertThat(asado.get("entradoKg").asText()).isEqualTo("5.000");
		// Sin ninguna venta de este corte, coalesce(..., 0) da escala 0, no numeric(8,3).
		assertThat(asado.get("vendidoKg").asText()).isEqualTo("0");
		assertThat(asado.get("stockKg").asText()).isEqualTo("5.000");
		assertThat(asado.get("quedaPoco").asBoolean()).isFalse();
	}

	@Test
	void unCorteQueDescuentaStockDeOtro_sumaSusVentasAlDestinoYNoAparecePorSuCuenta() throws Exception {
		// "Bife de chorizo" (pedido por chat): se vende con su propio precio, pero el
		// stock que se descuenta es el de "Asado" — el corte que realmente se cargó en
		// el despostado. No debe aparecer como fila propia en /stock.
		cargarDespostado(); // Vacío 10 kg, Asado 5 kg

		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		var bifeDeChorizo = corteService.crear(
				new CorteRequest("Bife de chorizo", 9040, "Trasero", null, null, null, "9000.00", corteAsadoId),
				DUENO_TEST_ID);
		UUID bifeDeChorizoId = bifeDeChorizo.id();
		ventaRepository.save(new VentaEntity(
				bifeDeChorizoId, new java.math.BigDecimal("2.000"), new java.math.BigDecimal("18000.00"),
				"codigo-de-prueba", UUID.randomUUID(), DUENO_TEST_ID, DUENO_TEST_ID, Instant.now()));
		ventaRepository.flush();
		jwtClaimsHolder.clear();

		String respuesta = mockMvc.perform(get("/api/v1/stock").with(jwtDeDueno()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode stock = objectMapper.readTree(respuesta);

		for (JsonNode fila : stock) {
			assertThat(fila.get("corteNombre").asText()).isNotEqualTo("Bife de chorizo");
		}
		JsonNode asado = filaDe(stock, "Asado");
		assertThat(asado.get("vendidoKg").asText()).isEqualTo("2.000");
		assertThat(asado.get("stockKg").asText()).isEqualTo("3.000"); // 5 - 2
	}

	@Test
	void empleadoDelMismoNegocio_puedeLeerStock() throws Exception {
		cargarDespostado();
		escanearVacio(1);

		jwtClaimsHolder.set("{\"sub\":\"" + EMPLEADO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoEmpleado(EMPLEADO_TEST_ID, "Empleado de StockControllerTest", DUENO_TEST_ID);
		jwtClaimsHolder.clear();

		String respuesta = mockMvc.perform(get("/api/v1/stock").with(jwtDeEmpleado()))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode stock = objectMapper.readTree(respuesta);

		assertThat(filaDe(stock, "Vacío").get("vendidoKg").asText()).isEqualTo("1.250");
	}
}
