package com.carniceria.despostado.service;

import com.carniceria.cortes.entity.CorteEntity;
import com.carniceria.cortes.repository.CorteRepository;
import com.carniceria.despostado.dto.CargarEntradaRequest;
import com.carniceria.despostado.dto.CorteKgDto;
import com.carniceria.despostado.dto.CorteKgEstimadoDto;
import com.carniceria.despostado.dto.EstimacionResponse;
import com.carniceria.despostado.dto.MediaResResponse;
import com.carniceria.despostado.dto.PerdidasDto;
import com.carniceria.despostado.dto.ResumenDto;
import com.carniceria.despostado.entity.DespostadoEntity;
import com.carniceria.despostado.entity.MediaResEntity;
import com.carniceria.despostado.entity.PerdidaEntity;
import com.carniceria.despostado.modelo.EstimacionCalculator;
import com.carniceria.despostado.modelo.Perdidas;
import com.carniceria.despostado.modelo.ResumenDespostado;
import com.carniceria.despostado.repository.DespostadoRepository;
import com.carniceria.despostado.repository.MediaResRepository;
import com.carniceria.despostado.repository.PerdidaRepository;
import com.carniceria.shared.BigDecimals;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaResService {

	// constitution.md, Principio V: toda fecha de este negocio es en hora Argentina.
	private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

	private final MediaResRepository mediaResRepository;
	private final DespostadoRepository despostadoRepository;
	private final PerdidaRepository perdidaRepository;
	private final CorteRepository corteRepository;

	public MediaResService(
			MediaResRepository mediaResRepository,
			DespostadoRepository despostadoRepository,
			PerdidaRepository perdidaRepository,
			CorteRepository corteRepository) {
		this.mediaResRepository = mediaResRepository;
		this.despostadoRepository = despostadoRepository;
		this.perdidaRepository = perdidaRepository;
		this.corteRepository = corteRepository;
	}

	@Transactional
	public MediaResResponse cargarEntrada(CargarEntradaRequest request, UUID usuarioId) {
		BigDecimal pesoKg = parsearPeso(request.pesoKg());
		BigDecimal precioKg = BigDecimals.parse(request.precioKg());
		validarCortes(request.cortesOVacio());

		MediaResEntity mediaRes = new MediaResEntity(
				LocalDate.now(ZONA_ARGENTINA), request.proveedor(), pesoKg, precioKg,
				usuarioId, Instant.now());
		mediaRes = mediaResRepository.save(mediaRes);

		guardarDespostadoYPerdidas(mediaRes.getId(), request.cortesOVacio(), request.perdidas());

		return construirRespuesta(mediaRes);
	}

	@Transactional
	public MediaResResponse actualizar(UUID id, CargarEntradaRequest request) {
		MediaResEntity mediaRes = mediaResRepository.findById(id)
				.orElseThrow(() -> new MediaResNoEncontradaException(id));
		BigDecimal pesoKg = parsearPeso(request.pesoKg());
		BigDecimal precioKg = BigDecimals.parse(request.precioKg());
		validarCortes(request.cortesOVacio());

		mediaRes.setProveedor(request.proveedor());
		mediaRes.setPesoKg(pesoKg);
		mediaRes.setPrecioKg(precioKg);

		despostadoRepository.deleteByMediaResId(id);
		perdidaRepository.deleteByMediaResId(id);
		guardarDespostadoYPerdidas(id, request.cortesOVacio(), request.perdidas());

		return construirRespuesta(mediaRes);
	}

	@Transactional
	public MediaResResponse buscarPorId(UUID id) {
		MediaResEntity mediaRes = mediaResRepository.findById(id)
				.orElseThrow(() -> new MediaResNoEncontradaException(id));
		return construirRespuesta(mediaRes);
	}

	@Transactional
	public List<MediaResResponse> listar(LocalDate desde, LocalDate hasta) {
		Instant desdeInstant = desde.atStartOfDay(ZONA_ARGENTINA).toInstant();
		Instant hastaInstant = hasta.plusDays(1).atStartOfDay(ZONA_ARGENTINA).toInstant().minusNanos(1);
		return mediaResRepository.findByCreadoEnBetweenOrderByCreadoEnDesc(desdeInstant, hastaInstant).stream()
				.map(this::construirRespuesta)
				.toList();
	}

	@Transactional
	public EstimacionResponse estimar(BigDecimal pesoKgNuevo, UUID usuarioId) {
		var historico = despostadoRepository.buscarHistoricoPorUsuario(usuarioId);
		if (historico.isEmpty()) {
			throw new SinHistorialException();
		}
		Map<UUID, BigDecimal> estimado = EstimacionCalculator.estimar(historico, pesoKgNuevo);
		List<CorteKgEstimadoDto> cortes = estimado.entrySet().stream()
				.map(e -> new CorteKgEstimadoDto(e.getKey(), BigDecimals.aTexto(e.getValue())))
				.toList();
		return new EstimacionResponse(cortes);
	}

	private BigDecimal parsearPeso(String pesoKgTexto) {
		BigDecimal pesoKg = BigDecimals.parse(pesoKgTexto);
		if (pesoKg == null || pesoKg.signum() <= 0) {
			throw new PesoInvalidoException();
		}
		return pesoKg;
	}

	private void validarCortes(List<CorteKgDto> cortes) {
		for (CorteKgDto corte : cortes) {
			BigDecimal kg = BigDecimals.parse(corte.kg());
			if (kg == null || kg.signum() <= 0) {
				throw new KgInvalidoException();
			}
			CorteEntity corteEntity = corteRepository.findById(corte.corteId())
					.orElseThrow(() -> new CorteInexistenteException(corte.corteId()));
			if (!corteEntity.isActivo()) {
				throw new CorteInexistenteException(corte.corteId());
			}
		}
	}

	private void guardarDespostadoYPerdidas(UUID mediaResId, List<CorteKgDto> cortes, PerdidasDto perdidasDto) {
		for (CorteKgDto corte : cortes) {
			despostadoRepository.save(
					new DespostadoEntity(mediaResId, corte.corteId(), BigDecimals.parse(corte.kg())));
		}
		if (perdidasDto == null) {
			return;
		}
		Perdidas perdidas = perdidasDto.aPerdidas();
		guardarPerdidaSiCorresponde(mediaResId, PerdidaEntity.Tipo.hueso, perdidas.hueso());
		guardarPerdidaSiCorresponde(mediaResId, PerdidaEntity.Tipo.grasa, perdidas.grasa());
		guardarPerdidaSiCorresponde(mediaResId, PerdidaEntity.Tipo.merma, perdidas.merma());
	}

	private void guardarPerdidaSiCorresponde(UUID mediaResId, PerdidaEntity.Tipo tipo, BigDecimal kg) {
		if (kg == null || kg.signum() == 0) {
			return;
		}
		if (kg.signum() < 0) {
			throw new KgInvalidoException();
		}
		perdidaRepository.save(new PerdidaEntity(mediaResId, tipo, kg));
	}

	private MediaResResponse construirRespuesta(MediaResEntity mediaRes) {
		List<DespostadoEntity> despostado = despostadoRepository.findByMediaResId(mediaRes.getId());
		List<PerdidaEntity> perdidasEntities = perdidaRepository.findByMediaResId(mediaRes.getId());

		Map<UUID, BigDecimal> kgPorCorte = despostado.stream()
				.collect(java.util.stream.Collectors.toMap(DespostadoEntity::getCorteId, DespostadoEntity::getKg));

		Perdidas perdidas = new Perdidas(
				buscarKgPorTipo(perdidasEntities, PerdidaEntity.Tipo.hueso),
				buscarKgPorTipo(perdidasEntities, PerdidaEntity.Tipo.grasa),
				buscarKgPorTipo(perdidasEntities, PerdidaEntity.Tipo.merma));

		ResumenDespostado resumen = new ResumenDespostado(mediaRes.getPesoKg(), mediaRes.getPrecioKg(), kgPorCorte,
				perdidas);

		List<CorteKgDto> despostadoDto = despostado.stream()
				.map(d -> new CorteKgDto(d.getCorteId(), BigDecimals.aTexto(d.getKg())))
				.toList();

		return new MediaResResponse(
				mediaRes.getId(), mediaRes.getFecha(), mediaRes.getProveedor(),
				BigDecimals.aTexto(mediaRes.getPesoKg()), BigDecimals.aTexto(mediaRes.getPrecioKg()),
				despostadoDto, PerdidasDto.de(perdidas), ResumenDto.de(resumen));
	}

	private BigDecimal buscarKgPorTipo(List<PerdidaEntity> perdidas, PerdidaEntity.Tipo tipo) {
		return perdidas.stream()
				.filter(p -> p.getTipo() == tipo)
				.map(PerdidaEntity::getKg)
				.findFirst()
				.orElse(BigDecimal.ZERO);
	}
}
