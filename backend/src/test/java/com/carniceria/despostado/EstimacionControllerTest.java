package com.carniceria.despostado;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carniceria.cortes.repository.CorteRepository;
import com.carniceria.cortes.service.CatalogoInicialService;
import com.carniceria.despostado.dto.CargarEntradaRequest;
import com.carniceria.despostado.dto.CorteKgDto;
import com.carniceria.despostado.service.MediaResService;
import com.carniceria.shared.NegocioTestFixtures;
import com.carniceria.shared.security.JwtClaimsHolder;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ver contracts/despostado-api.md, "GET /medias-reses/estimacion" (FR-113).
 *
 * Los dos usuarios de este test son cuentas reales de Supabase Auth sin perfil
 * permanente, no el "87b585e4..." de otros tests — ese pasó a ser la cuenta "admin" única
 * (V9__rol_admin.sql) y un admin no es dueño de ningún negocio (V10__multi_negocio.sql).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class EstimacionControllerTest {

	private static final UUID DUENO_TEST_ID = UUID.fromString("04d97faa-fd1c-42ce-9fa6-52c697733687");
	// Usuario real de Supabase Auth (ver PerfilControllerTest): medias_reses.creado_por
	// referencia auth.users(id), un UUID inventado rompe la FK.
	private static final UUID OTRO_USUARIO_ID = UUID.fromString("8890356c-29b6-473b-8e2d-6f0ff679c994");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MediaResService mediaResService;

	@Autowired
	private JwtClaimsHolder jwtClaimsHolder;

	@Autowired
	private NegocioTestFixtures negocioTestFixtures;

	@Autowired
	private CatalogoInicialService catalogoInicialService;

	@Autowired
	private CorteRepository corteRepository;

	private UUID corteAsadoId;

	@BeforeEach
	void prepararDuenoConCatalogo() {
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(DUENO_TEST_ID, "Dueño de EstimacionControllerTest");
		catalogoInicialService.sembrarSiHaceFalta(DUENO_TEST_ID);
		// PLU 11 = Asado, sembrado por CatalogoInicialService. Por JPA (no jdbcTemplate
		// crudo): un SELECT directo no dispara el auto-flush de Hibernate y no vería los
		// INSERT que sembrarSiHaceFalta todavía tiene sin volcar a la base.
		corteAsadoId = corteRepository.findByPlu(11).orElseThrow().getId();
		jwtClaimsHolder.clear();
	}

	private RequestPostProcessor jwtDeDueno() {
		return jwt().jwt(j -> j.subject(DUENO_TEST_ID.toString()).claim("role", "authenticated"));
	}

	@Test
	void sinNingunaEntradaCargada_devuelve409() throws Exception {
		mockMvc.perform(get("/api/v1/medias-reses/estimacion").with(jwtDeDueno())
						.param("pesoKg", "100.000"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("SIN_HISTORIAL"));
	}

	@Test
	void conUnaEntradaHistorica_estimaElPorcentajeEscaladoAlPesoNuevo() throws Exception {
		// Se inserta vía el service (no JdbcTemplate directo). Como esta llamada no pasa
		// por el filtro HTTP (JwtClaimsContextFilter), hay que simular su trabajo a mano
		// seteando el holder — si no, RlsSessionAspect no tiene nada que propagar y RLS
		// bloquea el insert (ver RlsPropagationIT, Bloque 2, para el mismo patrón).
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		CargarEntradaRequest entradaHistorica = new CargarEntradaRequest(
				null, "100.000", null,
				List.of(new CorteKgDto(corteAsadoId, "11.000")), // 11 % en la entrada histórica
				null);
		mediaResService.cargarEntrada(entradaHistorica, DUENO_TEST_ID);
		jwtClaimsHolder.clear();

		mockMvc.perform(get("/api/v1/medias-reses/estimacion").with(jwtDeDueno())
						.param("pesoKg", "80.000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cortes[0].corteId").value(corteAsadoId.toString()))
				.andExpect(jsonPath("$.cortes[0].kgEstimado").value("8.800")); // 11 % de 80 kg
	}

	private RequestPostProcessor jwtDeOtroUsuario() {
		return jwt().jwt(j -> j.subject(OTRO_USUARIO_ID.toString()).claim("role", "authenticated"));
	}

	@Test
	void elHistoricoDeOtroUsuario_noCuentaParaLaEstimacionPropia() throws Exception {
		// Cada carnicero suele trabajar siempre con la misma raza/proveedor: la estimación
		// tiene que basarse en el propio historial, no en el de otro usuario del sistema.
		// OTRO_USUARIO_ID es un negocio aparte (V10__multi_negocio.sql): tiene su propio
		// catálogo de cortes, no puede reusar corteAsadoId (es de otro tenant — RLS se lo
		// escondería igual si lo intentara, CORTE_INEXISTENTE).
		jwtClaimsHolder.set("{\"sub\":\"" + OTRO_USUARIO_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(OTRO_USUARIO_ID, "Otro negocio de prueba");
		catalogoInicialService.sembrarSiHaceFalta(OTRO_USUARIO_ID);
		UUID corteAsadoDeOtroUsuario = corteRepository.findByPlu(11).orElseThrow().getId();

		CargarEntradaRequest entradaDeOtroUsuario = new CargarEntradaRequest(
				null, "100.000", null,
				List.of(new CorteKgDto(corteAsadoDeOtroUsuario, "11.000")),
				null);
		mediaResService.cargarEntrada(entradaDeOtroUsuario, OTRO_USUARIO_ID);
		jwtClaimsHolder.clear();

		mockMvc.perform(get("/api/v1/medias-reses/estimacion").with(jwtDeDueno())
						.param("pesoKg", "80.000"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("SIN_HISTORIAL"));

		mockMvc.perform(get("/api/v1/medias-reses/estimacion").with(jwtDeOtroUsuario())
						.param("pesoKg", "80.000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cortes[0].kgEstimado").value("8.800"));
	}
}
