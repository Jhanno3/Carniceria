package com.carniceria.escaneo.service;

import com.carniceria.escaneo.dto.ConfigEtiquetaDto;
import com.carniceria.escaneo.modelo.ConfigEtiqueta;
import com.carniceria.escaneo.repository.ConfigEtiquetaRepository;
import com.carniceria.shared.error.AccesoDenegadoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ver contracts/control-diario-api.md, "GET/PUT /config-etiqueta" (FR-209). */
@Service
public class ConfigEtiquetaService {

	private final ConfigEtiquetaRepository configEtiquetaRepository;

	public ConfigEtiquetaService(ConfigEtiquetaRepository configEtiquetaRepository) {
		this.configEtiquetaRepository = configEtiquetaRepository;
	}

	@Transactional
	public ConfigEtiquetaDto obtener(UUID duenoId) {
		return configEtiquetaRepository.findById(duenoId)
				.map(ConfigEtiquetaDto::de)
				.orElseThrow(ConfigEtiquetaNoEncontradaException::new);
	}

	@Transactional
	public ConfigEtiquetaDto actualizar(UUID duenoId, ConfigEtiquetaDto request) {
		ConfigEtiqueta.TipoValor tipoValor = parsearTipoValor(request.tipoValor());
		validarRangos(request);

		int filasActualizadas = configEtiquetaRepository.actualizar(
				duenoId, request.prefijoDesde(), request.prefijoHasta(),
				request.inicioPlu(), request.largoPlu(), request.inicioValor(), request.largoValor(),
				tipoValor, request.decimales());
		if (filasActualizadas == 0) {
			// Existe pero RLS no deja escribirlo (ej. empleado) → 403, no 404. Si ni
			// siquiera existe (no debería pasar para un dueño aprobado), sí es 404.
			if (!configEtiquetaRepository.existsById(duenoId)) {
				throw new ConfigEtiquetaNoEncontradaException();
			}
			throw new AccesoDenegadoException();
		}
		return obtener(duenoId);
	}

	/**
	 * FR-209: solo valida que los rangos tengan sentido (prefijo creciente, largos
	 * positivos, PLU y valor sin pisarse dentro de los 13 dígitos) — no valida contra
	 * ningún código real, eso es responsabilidad del dueño al probar un escaneo después.
	 */
	private void validarRangos(ConfigEtiquetaDto dto) {
		if (dto.prefijoDesde() < 20 || dto.prefijoHasta() > 29 || dto.prefijoDesde() > dto.prefijoHasta()) {
			throw new ConfiguracionEtiquetaInvalidaException();
		}
		if (dto.inicioPlu() < 0 || dto.largoPlu() <= 0 || dto.inicioValor() < 0 || dto.largoValor() <= 0
				|| dto.decimales() < 0) {
			throw new ConfiguracionEtiquetaInvalidaException();
		}
		int finPlu = dto.inicioPlu() + dto.largoPlu();
		int finValor = dto.inicioValor() + dto.largoValor();
		boolean seSuperponen = dto.inicioPlu() < finValor && dto.inicioValor() < finPlu;
		if (seSuperponen) {
			throw new ConfiguracionEtiquetaInvalidaException();
		}
	}

	private ConfigEtiqueta.TipoValor parsearTipoValor(String valor) {
		try {
			return ConfigEtiqueta.TipoValor.valueOf(valor);
		} catch (IllegalArgumentException e) {
			throw new ConfiguracionEtiquetaInvalidaException();
		}
	}
}
