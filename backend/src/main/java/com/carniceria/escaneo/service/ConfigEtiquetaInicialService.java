package com.carniceria.escaneo.service;

import com.carniceria.escaneo.entity.ConfigEtiquetaEntity;
import com.carniceria.escaneo.modelo.ConfigEtiqueta;
import com.carniceria.escaneo.repository.ConfigEtiquetaRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra la config de etiqueta de ejemplo (especificacion-carniceria.md, sección 7) para
 * un dueño recién aprobado — mismo momento y mismo criterio que {@code CatalogoInicialService}
 * para los cortes (Fase 1): bajo la sesión del propio dueño, porque "para otro" no pasa RLS.
 */
@Service
public class ConfigEtiquetaInicialService {

	private final ConfigEtiquetaRepository configEtiquetaRepository;

	public ConfigEtiquetaInicialService(ConfigEtiquetaRepository configEtiquetaRepository) {
		this.configEtiquetaRepository = configEtiquetaRepository;
	}

	@Transactional
	public void sembrarSiHaceFalta(UUID duenoId) {
		if (configEtiquetaRepository.existsById(duenoId)) {
			return;
		}
		configEtiquetaRepository.save(
				new ConfigEtiquetaEntity(duenoId, 20, 29, 2, 5, 7, 5, ConfigEtiqueta.TipoValor.peso, 3));
		// Mismo motivo que CatalogoInicialService: id asignado (no autogenerado por la
		// base), Hibernate puede diferir el INSERT hasta el próximo flush.
		configEtiquetaRepository.flush();
	}
}
