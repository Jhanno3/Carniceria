package com.carniceria.despostado;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carniceria.despostado.dto.CargarEntradaRequest;
import com.carniceria.despostado.dto.CorteKgDto;
import com.carniceria.despostado.service.MediaResService;
import com.carniceria.shared.security.JwtClaimsHolder;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Ver contracts/despostado-api.md, "GET /medias-reses/estimacion" (FR-113). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class EstimacionControllerTest {

	private static final UUID DUENO_TEST_ID = UUID.fromString("87b585e4-f4e8-4d9a-858d-efb77058a49d");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MediaResService mediaResService;

	@Autowired
	private JwtClaimsHolder jwtClaimsHolder;

	private UUID corteAsadoId;

	@BeforeEach
	void buscarCorteDeEjemplo() {
		// PLU 11 = Asado, seedeado en V3__seed_cortes.sql.
		corteAsadoId = jdbcTemplate.queryForObject("select id from cortes where plu = 11", UUID.class);
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
}
