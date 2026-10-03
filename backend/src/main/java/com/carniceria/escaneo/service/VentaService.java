package com.carniceria.escaneo.service;

import com.carniceria.cortes.entity.CorteEntity;
import com.carniceria.cortes.repository.CorteRepository;
import com.carniceria.escaneo.dto.EscanearRequest;
import com.carniceria.escaneo.dto.VentaResponse;
import com.carniceria.escaneo.entity.ConfigEtiquetaEntity;
import com.carniceria.escaneo.entity.VentaEntity;
import com.carniceria.escaneo.modelo.DecodificadorEtiqueta;
import com.carniceria.escaneo.modelo.ResultadoDecodificacion;
import com.carniceria.escaneo.repository.ConfigEtiquetaRepository;
import com.carniceria.escaneo.repository.VentaRepository;
import com.carniceria.perfiles.entity.PerfilEntity;
import com.carniceria.perfiles.repository.PerfilRepository;
import com.carniceria.shared.error.AccesoDenegadoException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VentaService {

	// constitution.md, Principio V: toda fecha de este negocio es en hora Argentina.
	private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

	/** Resultado de un escaneo — distingue "nueva" (201) de "ya la tenía" (200), ver el controller. */
	public record ResultadoEscaneo(VentaResponse venta, boolean yaRegistrada) {
	}

	private final VentaRepository ventaRepository;
	private final CorteRepository corteRepository;
	private final ConfigEtiquetaRepository configEtiquetaRepository;
	private final PerfilRepository perfilRepository;

	public VentaService(VentaRepository ventaRepository, CorteRepository corteRepository,
			ConfigEtiquetaRepository configEtiquetaRepository, PerfilRepository perfilRepository) {
		this.ventaRepository = ventaRepository;
		this.corteRepository = corteRepository;
		this.configEtiquetaRepository = configEtiquetaRepository;
		this.perfilRepository = perfilRepository;
	}

	@Transactional
	public ResultadoEscaneo escanear(EscanearRequest request, UUID usuarioId) {
		var existente = ventaRepository.findByIdClienteLocal(request.idClienteLocal());
		if (existente.isPresent()) {
			return new ResultadoEscaneo(construirRespuesta(existente.get()), true);
		}

		UUID duenoId = resolverNegocioId(usuarioId);
		ConfigEtiquetaEntity config = configEtiquetaRepository.findById(duenoId)
				.orElseThrow(AccesoDenegadoException::new);

		ResultadoDecodificacion resultado = DecodificadorEtiqueta.decodificar(request.codigo(), config.aDominio());
		ResultadoDecodificacion.Exito exito = traducirOFallar(resultado);

		CorteEntity corte = corteRepository.findByPlu(exito.plu())
				.filter(CorteEntity::isActivo)
				.orElseThrow(PluInexistenteException::new);

		VentaEntity venta = new VentaEntity(
				corte.getId(), exito.kg(), request.codigo(), request.idClienteLocal(), usuarioId, duenoId,
				Instant.now());
		ventaRepository.save(venta);
		// Id generado en memoria (GenerationType.UUID): mismo motivo que CorteService.crear,
		// forzar el flush acá para que un rechazo de RLS se traduzca en este mismo pedido.
		ventaRepository.flush();
		return new ResultadoEscaneo(construirRespuesta(venta, corte.getNombre()), false);
	}

	@Transactional
	public List<VentaResponse> listar(LocalDate desde, LocalDate hasta, Integer limite) {
		Instant desdeInstant = desde.atStartOfDay(ZONA_ARGENTINA).toInstant();
		Instant hastaInstant = hasta.plusDays(1).atStartOfDay(ZONA_ARGENTINA).toInstant().minusNanos(1);
		Stream<VentaEntity> ventas = ventaRepository
				.findByFechaHoraBetweenOrderByFechaHoraDesc(desdeInstant, hastaInstant)
				.stream();
		if (limite != null) {
			ventas = ventas.limit(limite);
		}
		return ventas.map(this::construirRespuesta).toList();
	}

	private ResultadoDecodificacion.Exito traducirOFallar(ResultadoDecodificacion resultado) {
		return switch (resultado) {
			case ResultadoDecodificacion.Exito exito -> exito;
			case ResultadoDecodificacion.DigitoVerificadorInvalido ignored ->
					throw new DigitoVerificadorInvalidoException();
			case ResultadoDecodificacion.PrefijoInvalido ignored -> throw new PrefijoInvalidoException();
			case ResultadoDecodificacion.PesoCero ignored -> throw new PesoCeroException();
		};
	}

	/** El negocio de quien actúa: para un dueño es él mismo, para un empleado es quien lo invitó. */
	private UUID resolverNegocioId(UUID usuarioId) {
		return perfilRepository.findById(usuarioId)
				.map(PerfilEntity::getDuenoId)
				.filter(Objects::nonNull)
				.orElseThrow(AccesoDenegadoException::new);
	}

	private VentaResponse construirRespuesta(VentaEntity venta) {
		String corteNombre = corteRepository.findById(venta.getCorteId()).map(CorteEntity::getNombre).orElse(null);
		return construirRespuesta(venta, corteNombre);
	}

	private VentaResponse construirRespuesta(VentaEntity venta, String corteNombre) {
		return VentaResponse.de(venta, corteNombre);
	}
}
