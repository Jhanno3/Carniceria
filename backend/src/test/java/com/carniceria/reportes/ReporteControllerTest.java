package com.carniceria.reportes;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carniceria.despostado.entity.DespostadoEntity;
import com.carniceria.despostado.entity.MediaResEntity;
import com.carniceria.despostado.repository.DespostadoRepository;
import com.carniceria.despostado.repository.MediaResRepository;
import com.carniceria.shared.NegocioTestFixtures;
import com.carniceria.shared.security.JwtClaimsHolder;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

/** Ver contracts/reportes-api.md. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class ReporteControllerTest {

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
	private MediaResRepository mediaResRepository;

	@Autowired
	private DespostadoRepository despostadoRepository;

	private UUID corteVacioId;

	@BeforeEach
	void prepararNegocio() {
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(DUENO_TEST_ID, "Dueño de ReporteControllerTest");
		// Se crea un corte propio en vez de usar CatalogoInicialService: a los reportes no
		// les importa cuál corte se usó, solo el vendibleKg total de cada media res.
		corteVacioId = crearCorteDirecto(DUENO_TEST_ID);
		jwtClaimsHolder.clear();
	}

	private RequestPostProcessor jwtDeDueno() {
		return jwt().jwt(j -> j.subject(DUENO_TEST_ID.toString()).claim("role", "authenticated"));
	}

	private UUID crearCorteDirecto(UUID duenoId) {
		var corte = new com.carniceria.cortes.entity.CorteEntity(
				"Corte de prueba", 1, com.carniceria.cortes.entity.CorteEntity.Cuarto.Ambos,
				com.carniceria.cortes.entity.CorteEntity.TipoProducto.Vacuno, null, true, null, duenoId, null);
		corte = corteRepositoryGuardar(corte);
		return corte.getId();
	}

	@Autowired
	private com.carniceria.cortes.repository.CorteRepository corteRepository;

	private com.carniceria.cortes.entity.CorteEntity corteRepositoryGuardar(com.carniceria.cortes.entity.CorteEntity corte) {
		corte = corteRepository.save(corte);
		corteRepository.flush();
		return corte;
	}

	private UUID crearMediaResDirecta(
			LocalDate fecha, String proveedor, MediaResEntity.Categoria categoria, BigDecimal pesoKg,
			BigDecimal precioKg, BigDecimal vendibleKg, UUID duenoId) {
		MediaResEntity mediaRes = mediaResRepository.save(
				new MediaResEntity(fecha, proveedor, pesoKg, precioKg, categoria, MediaResEntity.TipoEntrada.MediaRes,
						duenoId, Instant.now()));
		mediaResRepository.flush();
		despostadoRepository.save(new DespostadoEntity(mediaRes.getId(), corteVacioId, vendibleKg));
		despostadoRepository.flush();
		return mediaRes.getId();
	}

	@Test
	void porProveedor_agrupaCorrectamenteYDejaLosSinProveedorAlFinal() throws Exception {
		LocalDate hoy = LocalDate.now();
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		crearMediaResDirecta(hoy, "Frigorífico Sur", null, new BigDecimal("100.000"), new BigDecimal("1000.00"),
				new BigDecimal("90.000"), DUENO_TEST_ID);
		crearMediaResDirecta(hoy, "Frigorífico Norte", null, new BigDecimal("50.000"), new BigDecimal("2000.00"),
				new BigDecimal("40.000"), DUENO_TEST_ID);
		crearMediaResDirecta(hoy, null, null, new BigDecimal("20.000"), null, new BigDecimal("15.000"), DUENO_TEST_ID);
		jwtClaimsHolder.clear();

		mockMvc.perform(get("/api/v1/reportes/por-proveedor").with(jwtDeDueno())
						.param("desde", hoy.toString()).param("hasta", hoy.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].proveedor").value("Frigorífico Norte"))
				.andExpect(jsonPath("$[0].cantidadEntradas").value(1))
				.andExpect(jsonPath("$[0].rendimientoPromedioPorc").value("80.00"))
				.andExpect(jsonPath("$[0].costoKgVendiblePromedio").value("2500"))
				.andExpect(jsonPath("$[1].proveedor").value("Frigorífico Sur"))
				.andExpect(jsonPath("$[1].rendimientoPromedioPorc").value("90.00"))
				.andExpect(jsonPath("$[1].costoKgVendiblePromedio").value("1111"))
				.andExpect(jsonPath("$[2].proveedor").isEmpty())
				.andExpect(jsonPath("$[2].rendimientoPromedioPorc").value("75.00"))
				.andExpect(jsonPath("$[2].costoKgVendiblePromedio").isEmpty());
	}

	@Test
	void porCategoria_agrupaCorrectamenteYDejaLasSinCategoriaAlFinal() throws Exception {
		LocalDate hoy = LocalDate.now();
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		crearMediaResDirecta(hoy, null, MediaResEntity.Categoria.Novillo, new BigDecimal("100.000"),
				new BigDecimal("1000.00"), new BigDecimal("90.000"), DUENO_TEST_ID);
		crearMediaResDirecta(hoy, null, null, new BigDecimal("20.000"), null, new BigDecimal("15.000"), DUENO_TEST_ID);
		jwtClaimsHolder.clear();

		mockMvc.perform(get("/api/v1/reportes/por-categoria").with(jwtDeDueno())
						.param("desde", hoy.toString()).param("hasta", hoy.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].categoria").value("Novillo"))
				.andExpect(jsonPath("$[0].rendimientoPromedioPorc").value("90.00"))
				.andExpect(jsonPath("$[1].categoria").isEmpty())
				.andExpect(jsonPath("$[1].rendimientoPromedioPorc").value("75.00"));
	}

	@Test
	void porPeriodo_agrupaPorSemanaYSumaLasQueCaenEnElMismoBucket() throws Exception {
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		// Lunes 2024-01-01 y martes 2024-01-02: misma semana (bucket 2024-01-01).
		crearMediaResDirecta(LocalDate.of(2024, 1, 1), null, null, new BigDecimal("100.000"), null,
				new BigDecimal("90.000"), DUENO_TEST_ID);
		crearMediaResDirecta(LocalDate.of(2024, 1, 2), null, null, new BigDecimal("50.000"), null,
				new BigDecimal("40.000"), DUENO_TEST_ID);
		// Lunes siguiente, 2024-01-08: otro bucket.
		crearMediaResDirecta(LocalDate.of(2024, 1, 8), null, null, new BigDecimal("10.000"), null,
				new BigDecimal("5.000"), DUENO_TEST_ID);
		jwtClaimsHolder.clear();

		mockMvc.perform(get("/api/v1/reportes/por-periodo").with(jwtDeDueno())
						.param("desde", "2024-01-01").param("hasta", "2024-01-08").param("periodo", "semana"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].periodoInicio").value("2024-01-01"))
				.andExpect(jsonPath("$[0].cantidadEntradas").value(2))
				.andExpect(jsonPath("$[1].periodoInicio").value("2024-01-08"))
				.andExpect(jsonPath("$[1].cantidadEntradas").value(1));
	}

	@Test
	void porPeriodo_conPeriodoInvalido_devuelve400() throws Exception {
		mockMvc.perform(get("/api/v1/reportes/por-periodo").with(jwtDeDueno())
						.param("desde", "2024-01-01").param("hasta", "2024-01-08").param("periodo", "anual"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("PERIODO_INVALIDO"));
	}

	@Test
	void rangoSinDatos_devuelve200ConListaVacia() throws Exception {
		mockMvc.perform(get("/api/v1/reportes/por-proveedor").with(jwtDeDueno())
						.param("desde", "2020-01-01").param("hasta", "2020-01-02"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void empleado_noVeNingunaEntradaDelReporte() throws Exception {
		LocalDate hoy = LocalDate.now();
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		crearMediaResDirecta(hoy, "Proveedor X", null, new BigDecimal("100.000"), new BigDecimal("1000.00"),
				new BigDecimal("90.000"), DUENO_TEST_ID);
		jwtClaimsHolder.clear();

		jwtClaimsHolder.set("{\"sub\":\"" + EMPLEADO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoEmpleado(EMPLEADO_TEST_ID, "Empleado de ReporteControllerTest", DUENO_TEST_ID);
		jwtClaimsHolder.clear();

		RequestPostProcessor jwtDeEmpleado = jwt().jwt(
				j -> j.subject(EMPLEADO_TEST_ID.toString()).claim("role", "authenticated"));
		mockMvc.perform(get("/api/v1/reportes/por-proveedor").with(jwtDeEmpleado)
						.param("desde", hoy.toString()).param("hasta", hoy.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void unaMediaResDeOtroNegocio_noContaminaElReportePropio() throws Exception {
		LocalDate hoy = LocalDate.now();
		jwtClaimsHolder.set("{\"sub\":\"" + OTRO_DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(OTRO_DUENO_TEST_ID, "Otro negocio de ReporteControllerTest");
		UUID corteDelOtro = crearCorteDirecto(OTRO_DUENO_TEST_ID);
		mediaResRepository.save(
				new MediaResEntity(hoy, "Proveedor del otro negocio", new BigDecimal("100.000"), null, null,
						MediaResEntity.TipoEntrada.MediaRes, OTRO_DUENO_TEST_ID, Instant.now()));
		mediaResRepository.flush();
		jwtClaimsHolder.clear();

		mockMvc.perform(get("/api/v1/reportes/por-proveedor").with(jwtDeDueno())
						.param("desde", hoy.toString()).param("hasta", hoy.toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}
}
