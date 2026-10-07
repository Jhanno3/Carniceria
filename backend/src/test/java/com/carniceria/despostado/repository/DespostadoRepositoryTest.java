package com.carniceria.despostado.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.carniceria.cortes.dto.CorteRequest;
import com.carniceria.cortes.entity.CorteEntity;
import com.carniceria.cortes.repository.CorteRepository;
import com.carniceria.cortes.service.CatalogoInicialService;
import com.carniceria.cortes.service.CorteService;
import com.carniceria.despostado.entity.DespostadoEntity;
import com.carniceria.despostado.entity.MediaResEntity;
import com.carniceria.despostado.modelo.IngresoCortesPorMediaRes;
import com.carniceria.despostado.modelo.VendibleKgPorMediaRes;
import com.carniceria.shared.NegocioTestFixtures;
import com.carniceria.shared.security.JwtClaimsHolder;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

/** Ver plan-fase4.md, 3.5: vendibleKg agregado por media res, sin repetir ResumenDespostado. */
@SpringBootTest
@Transactional
@Rollback
class DespostadoRepositoryTest {

	private static final UUID DUENO_TEST_ID = UUID.fromString("04d97faa-fd1c-42ce-9fa6-52c697733687");

	@Autowired
	private JwtClaimsHolder jwtClaimsHolder;

	@Autowired
	private NegocioTestFixtures negocioTestFixtures;

	@Autowired
	private CatalogoInicialService catalogoInicialService;

	@Autowired
	private CorteRepository corteRepository;

	@Autowired
	private MediaResRepository mediaResRepository;

	@Autowired
	private DespostadoRepository despostadoRepository;

	@Autowired
	private CorteService corteService;

	private UUID corteVacioId;
	private UUID corteAsadoId;

	@BeforeEach
	void prepararNegocio() {
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");
		negocioTestFixtures.registrarComoDueno(DUENO_TEST_ID, "Dueño de DespostadoRepositoryTest");
		catalogoInicialService.sembrarSiHaceFalta(DUENO_TEST_ID);
		corteVacioId = corteRepository.findByPlu(12).orElseThrow().getId(); // Vacío
		corteAsadoId = corteRepository.findByPlu(11).orElseThrow().getId(); // Asado
		jwtClaimsHolder.clear();
	}

	@Test
	void sumaElVendibleKgDeCadaMediaRes_sinProductoCartesiano() {
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");

		MediaResEntity mediaRes1 = mediaResRepository.save(new MediaResEntity(
				LocalDate.now(), "Proveedor A", new BigDecimal("100.000"), new BigDecimal("5000.00"), null,
				MediaResEntity.TipoEntrada.MediaRes, DUENO_TEST_ID, Instant.now()));
		MediaResEntity mediaRes2 = mediaResRepository.save(new MediaResEntity(
				LocalDate.now(), "Proveedor B", new BigDecimal("50.000"), new BigDecimal("4000.00"), null,
				MediaResEntity.TipoEntrada.MediaRes, DUENO_TEST_ID, Instant.now()));
		mediaResRepository.flush();

		// mediaRes1 tiene 2 filas de despostado (debe sumarlas, no multiplicarlas entre sí
		// ni con las de mediaRes2 — mismo tipo de bug que V15__stock_por_corte.sql, Fase 2).
		despostadoRepository.save(new DespostadoEntity(mediaRes1.getId(), corteVacioId, new BigDecimal("10.000")));
		despostadoRepository.save(new DespostadoEntity(mediaRes1.getId(), corteAsadoId, new BigDecimal("20.000")));
		despostadoRepository.save(new DespostadoEntity(mediaRes2.getId(), corteVacioId, new BigDecimal("15.000")));
		despostadoRepository.flush();

		List<VendibleKgPorMediaRes> resultado = despostadoRepository.sumarVendibleKgPorMediaRes(
				List.of(mediaRes1.getId(), mediaRes2.getId()));

		assertThat(resultado).hasSize(2);
		assertThat(buscar(resultado, mediaRes1.getId())).isEqualByComparingTo("30.000");
		assertThat(buscar(resultado, mediaRes2.getId())).isEqualByComparingTo("15.000");

		jwtClaimsHolder.clear();
	}

	private BigDecimal buscar(List<VendibleKgPorMediaRes> resultado, UUID mediaResId) {
		return resultado.stream()
				.filter(r -> r.mediaResId().equals(mediaResId))
				.findFirst()
				.orElseThrow()
				.vendibleKg();
	}

	@Test
	void sumaKgYValorVentaPorMediaRes_soloCuentaLosCortesConPrecioVenta() {
		jwtClaimsHolder.set("{\"sub\":\"" + DUENO_TEST_ID + "\",\"role\":\"authenticated\"}");

		// Ejemplo canónico de la especificación: media res de 100 kg, 81 kg vendibles. De
		// esos 81 kg, 60 son de un corte con precioVenta=9000 y 21 de otro sin precioVenta.
		fijarPrecioVenta(corteVacioId, "9000.00");
		// corteAsadoId se deja sin precioVenta a propósito.

		MediaResEntity mediaRes = mediaResRepository.save(new MediaResEntity(
				LocalDate.now(), "Proveedor A", new BigDecimal("100.000"), new BigDecimal("5000.00"), null,
				MediaResEntity.TipoEntrada.MediaRes, DUENO_TEST_ID, Instant.now()));
		mediaResRepository.flush();
		despostadoRepository.save(new DespostadoEntity(mediaRes.getId(), corteVacioId, new BigDecimal("60.000")));
		despostadoRepository.save(new DespostadoEntity(mediaRes.getId(), corteAsadoId, new BigDecimal("21.000")));
		despostadoRepository.flush();

		List<IngresoCortesPorMediaRes> resultado = despostadoRepository.sumarKgYValorVentaPorMediaRes(
				List.of(mediaRes.getId()));

		assertThat(resultado).hasSize(1);
		IngresoCortesPorMediaRes ingreso = resultado.get(0);
		assertThat(ingreso.mediaResId()).isEqualTo(mediaRes.getId());
		assertThat(ingreso.kgConPrecioVenta()).isEqualByComparingTo("60.000");
		assertThat(ingreso.importeConPrecioVenta()).isEqualByComparingTo("540000.00");

		jwtClaimsHolder.clear();
	}

	private void fijarPrecioVenta(UUID corteId, String precioVenta) {
		CorteEntity corte = corteRepository.findById(corteId).orElseThrow();
		corteService.actualizar(corteId, new CorteRequest(
				corte.getNombre(), corte.getPlu(), corte.getCuarto().name(), corte.getTipoProducto().name(),
				corte.getZonaMapa(), corte.isActivo(), precioVenta));
	}
}
