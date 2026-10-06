package com.carniceria.reportes.service;

import com.carniceria.despostado.entity.MediaResEntity;
import com.carniceria.despostado.modelo.IngresoCortesPorMediaRes;
import com.carniceria.despostado.modelo.VendibleKgPorMediaRes;
import com.carniceria.despostado.repository.DespostadoRepository;
import com.carniceria.despostado.repository.MediaResRepository;
import com.carniceria.reportes.dto.ReporteCategoriaItem;
import com.carniceria.reportes.dto.ReportePeriodoItem;
import com.carniceria.reportes.dto.ReporteProveedorItem;
import com.carniceria.reportes.modelo.AgregadorRendimiento;
import com.carniceria.reportes.modelo.AgregadorRendimiento.Entrada;
import com.carniceria.reportes.modelo.AgregadorRendimiento.Resultado;
import com.carniceria.reportes.modelo.CalculadorPeriodo;
import com.carniceria.reportes.modelo.CalculadorPeriodo.Periodo;
import com.carniceria.shared.BigDecimals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ver contracts/reportes-api.md y plan-fase4.md (3.1/3.2/3.5). No hay chequeo de rol acá:
 * la RLS de medias_reses/despostado (is_dueno() + mi_negocio_id(), desde
 * V11__fix_dueno_todo_exige_rol.sql) ya deja a un empleado sin ninguna fila, sin
 * necesidad de duplicar esa regla en Java.
 */
@Service
public class ReporteService {

	private final MediaResRepository mediaResRepository;
	private final DespostadoRepository despostadoRepository;

	public ReporteService(MediaResRepository mediaResRepository, DespostadoRepository despostadoRepository) {
		this.mediaResRepository = mediaResRepository;
		this.despostadoRepository = despostadoRepository;
	}

	@Transactional
	public List<ReporteProveedorItem> porProveedor(LocalDate desde, LocalDate hasta) {
		List<MediaResEntity> medias = mediaResRepository.findByFechaBetween(desde, hasta);
		Map<UUID, BigDecimal> vendibleKgPorId = cargarVendibleKgPorMediaRes(medias);
		Map<UUID, IngresoCortesPorMediaRes> ingresoPorId = cargarIngresoCortesPorMediaRes(medias);

		// Collectors.groupingBy rechaza con NPE una clave null (plan-fase4.md, 3.3 pide
		// agrupar las entradas sin proveedor aparte, no excluirlas) — se envuelve en
		// Optional para agrupar igual, sin arriesgar una colisión con un valor real como
		// pasaría con un sentinel tipo "" o "Sin proveedor".
		Map<Optional<String>, List<MediaResEntity>> porProveedor = medias.stream()
				.collect(Collectors.groupingBy(m -> Optional.ofNullable(m.getProveedor())));

		return porProveedor.entrySet().stream()
				.map(grupo -> {
					Resultado resultado = agregar(grupo.getValue(), vendibleKgPorId, ingresoPorId);
					return new ReporteProveedorItem(grupo.getKey().orElse(null), resultado.cantidadEntradas(),
							BigDecimals.aTexto(resultado.rendimientoPromedioPorc()),
							BigDecimals.aTexto(resultado.costoKgVendiblePromedio()),
							BigDecimals.aTexto(resultado.beneficioPorKgVendiblePromedio()));
				})
				.sorted(Comparator.comparing(
						ReporteProveedorItem::proveedor, Comparator.nullsLast(Comparator.naturalOrder())))
				.toList();
	}

	@Transactional
	public List<ReporteCategoriaItem> porCategoria(LocalDate desde, LocalDate hasta) {
		List<MediaResEntity> medias = mediaResRepository.findByFechaBetween(desde, hasta);
		Map<UUID, BigDecimal> vendibleKgPorId = cargarVendibleKgPorMediaRes(medias);
		Map<UUID, IngresoCortesPorMediaRes> ingresoPorId = cargarIngresoCortesPorMediaRes(medias);

		Map<Optional<String>, List<MediaResEntity>> porCategoria = medias.stream()
				.collect(Collectors.groupingBy(
						m -> Optional.ofNullable(m.getCategoria()).map(Enum::name)));

		return porCategoria.entrySet().stream()
				.map(grupo -> {
					Resultado resultado = agregar(grupo.getValue(), vendibleKgPorId, ingresoPorId);
					return new ReporteCategoriaItem(grupo.getKey().orElse(null), resultado.cantidadEntradas(),
							BigDecimals.aTexto(resultado.rendimientoPromedioPorc()),
							BigDecimals.aTexto(resultado.costoKgVendiblePromedio()),
							BigDecimals.aTexto(resultado.beneficioPorKgVendiblePromedio()));
				})
				.sorted(Comparator.comparing(
						ReporteCategoriaItem::categoria, Comparator.nullsLast(Comparator.naturalOrder())))
				.toList();
	}

	@Transactional
	public List<ReportePeriodoItem> porPeriodo(LocalDate desde, LocalDate hasta, String periodoTexto) {
		Periodo periodo = parsearPeriodo(periodoTexto);
		List<MediaResEntity> medias = mediaResRepository.findByFechaBetween(desde, hasta);
		Map<UUID, BigDecimal> vendibleKgPorId = cargarVendibleKgPorMediaRes(medias);
		Map<UUID, IngresoCortesPorMediaRes> ingresoPorId = cargarIngresoCortesPorMediaRes(medias);

		Map<LocalDate, List<MediaResEntity>> porPeriodo = medias.stream()
				.collect(Collectors.groupingBy(m -> CalculadorPeriodo.inicioDelBucket(m.getFecha(), periodo)));

		return porPeriodo.entrySet().stream()
				.map(grupo -> {
					Resultado resultado = agregar(grupo.getValue(), vendibleKgPorId, ingresoPorId);
					return new ReportePeriodoItem(grupo.getKey().toString(), resultado.cantidadEntradas(),
							BigDecimals.aTexto(resultado.rendimientoPromedioPorc()),
							BigDecimals.aTexto(resultado.costoKgVendiblePromedio()),
							BigDecimals.aTexto(resultado.beneficioPorKgVendiblePromedio()));
				})
				.sorted(Comparator.comparing(ReportePeriodoItem::periodoInicio))
				.toList();
	}

	private Resultado agregar(List<MediaResEntity> medias, Map<UUID, BigDecimal> vendibleKgPorId,
			Map<UUID, IngresoCortesPorMediaRes> ingresoPorId) {
		List<Entrada> entradas = medias.stream()
				.map(m -> {
					IngresoCortesPorMediaRes ingreso = ingresoPorId.get(m.getId());
					return new Entrada(
							m.getPesoKg(), m.getPrecioKg(), vendibleKgPorId.getOrDefault(m.getId(), BigDecimal.ZERO),
							ingreso == null ? null : ingreso.kgConPrecioVenta(),
							ingreso == null ? null : ingreso.importeConPrecioVenta());
				})
				.toList();
		return AgregadorRendimiento.agregar(entradas);
	}

	private Map<UUID, BigDecimal> cargarVendibleKgPorMediaRes(List<MediaResEntity> medias) {
		if (medias.isEmpty()) {
			return Map.of();
		}
		List<UUID> ids = medias.stream().map(MediaResEntity::getId).toList();
		return despostadoRepository.sumarVendibleKgPorMediaRes(ids).stream()
				.collect(Collectors.toMap(VendibleKgPorMediaRes::mediaResId, VendibleKgPorMediaRes::vendibleKg));
	}

	private Map<UUID, IngresoCortesPorMediaRes> cargarIngresoCortesPorMediaRes(List<MediaResEntity> medias) {
		if (medias.isEmpty()) {
			return Map.of();
		}
		List<UUID> ids = medias.stream().map(MediaResEntity::getId).toList();
		return despostadoRepository.sumarKgYValorVentaPorMediaRes(ids).stream()
				.collect(Collectors.toMap(IngresoCortesPorMediaRes::mediaResId, ingreso -> ingreso));
	}

	private Periodo parsearPeriodo(String texto) {
		try {
			return Periodo.valueOf(texto);
		} catch (IllegalArgumentException | NullPointerException e) {
			throw new PeriodoInvalidoException(texto);
		}
	}
}
