package com.carniceria.escaneo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carniceria.cortes.service.CatalogoInicialService;
import com.carniceria.escaneo.service.ConfigEtiquetaInicialService;
import com.carniceria.shared.NegocioTestFixtures;
import com.carniceria.shared.security.JwtClaimsHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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

/**
 * Ver contracts/control-diario-api.md, "POST /ventas" y "GET /ventas". Las identidades son
 * cuentas reales de Supabase Auth sin perfil permanente (mismo criterio que el resto de
 * Fase 1/2) — "87b585e4..." se repone a "admin" solo al final, al salir del rollback.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class VentaControllerTest {

	private static final UUID DUENO_TEST_ID = UUID.fromString("04d97faa-fd1c-42ce-9fa6-52c697733687");
	private static final UUID EMPLEADO_TEST_ID = UUID.fromString("8890356c-29b6-473b-8e2d-6f0ff679c994");
	private static final UUID OTRO_DUENO_TEST_ID = UUID.fromString("87b585e4-f4e8-4d9a-858d-efb77058a49d");

	// PLU 12 = Vacío (CatalogoInicialService), config de ejemplo = la de la especificación.
	private static final String CODIGO_VALIDO = "2000012012501"; // 1,250 kg
	private static final String CODIGO_DIGITO_INVALIDO = "2000012012509";
	private static final String CODIGO_PREFIJO_INVALIDO = "1900034050004"; // prefijo 19
	private static final String CODIGO_PREFIJO_Y_DIGITO_INVALIDOS = "1900034050005";
	private static final String CODIGO_PESO_CERO = "2000012000003"; // PLU 12, valor 00000
	private static final String CODIGO_PLU_INEXISTENTE = "2000099010001"; // PLU 99, no sembrado

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
		negocioTestFixtures.registrarComoDueno(DUENO_TEST_ID, "Dueño de VentaControllerTest");
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

	private Map<String, Object> cuerpoDeEscaneo(String codigo, UUID idClienteLocal) {
		return Map.of("codigo", codigo, "idClienteLocal", idClienteLocal.toString());
	}

	@Test
	void escanear_conCodigoValido_devuelve201ConVentaRegistrada() throws Exception {
		mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(cuerpoDeEscaneo(CODIGO_VALIDO, UUID.randomUUID()))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.corteNombre").value("Vacío"))
				.andExpect(jsonPath("$.kg").value("1.250"))
				.andExpect(jsonPath("$.codigoLeido").value(CODIGO_VALIDO))
				.andExpect(jsonPath("$.anulada").value(false));
	}

	@Test
	void escanear_conMismoIdClienteLocalRepetido_devuelve200SinDuplicar() throws Exception {
		UUID idClienteLocal = UUID.randomUUID();
		Map<String, Object> cuerpo = cuerpoDeEscaneo(CODIGO_VALIDO, idClienteLocal);

		mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(cuerpo)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(cuerpo)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/ventas").with(jwtDeDueno())
						.param("desde", LocalDate.now().toString())
						.param("hasta", LocalDate.now().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void escanear_conDigitoVerificadorInvalido_devuelve400() throws Exception {
		mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								cuerpoDeEscaneo(CODIGO_DIGITO_INVALIDO, UUID.randomUUID()))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("DIGITO_VERIFICADOR_INVALIDO"));
	}

	@Test
	void escanear_conPrefijoInvalido_devuelve400() throws Exception {
		mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								cuerpoDeEscaneo(CODIGO_PREFIJO_INVALIDO, UUID.randomUUID()))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("PREFIJO_INVALIDO"));
	}

	@Test
	void escanear_conPrefijoYDigitoInvalidos_priorizaDigitoVerificador() throws Exception {
		mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								cuerpoDeEscaneo(CODIGO_PREFIJO_Y_DIGITO_INVALIDOS, UUID.randomUUID()))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("DIGITO_VERIFICADOR_INVALIDO"));
	}

	@Test
	void escanear_conPesoCero_devuelve400() throws Exception {
		mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(cuerpoDeEscaneo(CODIGO_PESO_CERO, UUID.randomUUID()))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("PESO_CERO"));
	}

	@Test
	void escanear_conPluInexistente_devuelve400() throws Exception {
		mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								cuerpoDeEscaneo(CODIGO_PLU_INEXISTENTE, UUID.randomUUID()))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("PLU_INEXISTENTE"));
	}

	@Test
	void empleadoDelMismoNegocio_puedeEscanearYElDuenoVeLaVenta() throws Exception {
		jwtClaimsHolder.set("{\"sub\":\"" + EMPLEADO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoEmpleado(EMPLEADO_TEST_ID, "Empleado de VentaControllerTest", DUENO_TEST_ID);
		jwtClaimsHolder.clear();

		mockMvc.perform(post("/api/v1/ventas").with(jwtDeEmpleado())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(cuerpoDeEscaneo(CODIGO_VALIDO, UUID.randomUUID()))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.corteNombre").value("Vacío"))
				.andExpect(jsonPath("$.precioKg").doesNotExist());

		mockMvc.perform(get("/api/v1/ventas").with(jwtDeDueno())
						.param("desde", LocalDate.now().toString())
						.param("hasta", LocalDate.now().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void listarVentas_masRecientesPrimeroYConLimite() throws Exception {
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(cuerpoDeEscaneo(CODIGO_VALIDO, UUID.randomUUID()))))
					.andExpect(status().isCreated());
		}

		mockMvc.perform(get("/api/v1/ventas").with(jwtDeDueno())
						.param("desde", LocalDate.now().toString())
						.param("hasta", LocalDate.now().toString())
						.param("limite", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));

		mockMvc.perform(get("/api/v1/ventas").with(jwtDeDueno())
						.param("desde", LocalDate.now().minusDays(1).toString())
						.param("hasta", LocalDate.now().minusDays(1).toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void ventaDeUnNegocio_noApareceEnElListadoDeOtroNegocio() throws Exception {
		jwtClaimsHolder.set("{\"sub\":\"" + OTRO_DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(OTRO_DUENO_TEST_ID, "Otro negocio de VentaControllerTest");
		catalogoInicialService.sembrarSiHaceFalta(OTRO_DUENO_TEST_ID);
		configEtiquetaInicialService.sembrarSiHaceFalta(OTRO_DUENO_TEST_ID);
		jwtClaimsHolder.clear();

		mockMvc.perform(post("/api/v1/ventas").with(jwtDeDueno())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(cuerpoDeEscaneo(CODIGO_VALIDO, UUID.randomUUID()))))
				.andExpect(status().isCreated());

		RequestPostProcessor jwtDeOtroDueno = jwt().jwt(
				j -> j.subject(OTRO_DUENO_TEST_ID.toString()).claim("role", "authenticated"));
		mockMvc.perform(get("/api/v1/ventas").with(jwtDeOtroDueno)
						.param("desde", LocalDate.now().toString())
						.param("hasta", LocalDate.now().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}
}
