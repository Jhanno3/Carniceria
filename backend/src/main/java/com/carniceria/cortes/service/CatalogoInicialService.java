package com.carniceria.cortes.service;

import com.carniceria.cortes.entity.CorteEntity;
import com.carniceria.cortes.entity.CorteEntity.Cuarto;
import com.carniceria.cortes.repository.CorteRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra el catálogo de ejemplo (especificacion-carniceria.md, sección 2.2, menos los 4
 * cortes sacados en V8) para un dueño recién aprobado, así no arranca con la pantalla de
 * Despostado vacía. Se llama desde {@code PerfilService} bajo la sesión del propio dueño
 * (RLS exige {@code dueno_id = mi_negocio_id()}; sembrar "para otro" desde una sesión de
 * admin no pasaría esa política — ver V10__multi_negocio.sql).
 */
@Service
public class CatalogoInicialService {

	private record CorteDeEjemplo(String nombre, int plu, Cuarto cuarto, String zonaMapa) {
	}

	private static final List<CorteDeEjemplo> CATALOGO_DE_EJEMPLO = List.of(
			new CorteDeEjemplo("Cuadril", 1, Cuarto.Trasero, "cuadril"),
			new CorteDeEjemplo("Lomo", 2, Cuarto.Trasero, "lomo"),
			new CorteDeEjemplo("Bife angosto", 3, Cuarto.Trasero, "bifeAngosto"),
			new CorteDeEjemplo("Nalga", 4, Cuarto.Trasero, "nalga"),
			new CorteDeEjemplo("Cuadrada", 5, Cuarto.Trasero, "nalga"),
			new CorteDeEjemplo("Tapa de nalga", 6, Cuarto.Trasero, "nalga"),
			new CorteDeEjemplo("Bola de lomo", 7, Cuarto.Trasero, "bola"),
			new CorteDeEjemplo("Peceto", 8, Cuarto.Trasero, "bola"),
			new CorteDeEjemplo("Vacío", 12, Cuarto.Trasero, "vacio"),
			new CorteDeEjemplo("Matambre", 9, Cuarto.Trasero, "matambre"),
			new CorteDeEjemplo("Osobuco", 10, Cuarto.Ambos, "osobuco"),
			new CorteDeEjemplo("Asado", 11, Cuarto.Delantero, "asado"),
			new CorteDeEjemplo("Tapa de asado", 13, Cuarto.Delantero, "asado"),
			new CorteDeEjemplo("Falda", 14, Cuarto.Delantero, "falda"),
			new CorteDeEjemplo("Bife ancho", 15, Cuarto.Delantero, "bifeAncho"),
			new CorteDeEjemplo("Paleta", 17, Cuarto.Delantero, "paleta"),
			new CorteDeEjemplo("Carne picada / recortes", 21, Cuarto.Ambos, null));

	private final CorteRepository corteRepository;

	public CatalogoInicialService(CorteRepository corteRepository) {
		this.corteRepository = corteRepository;
	}

	/** No hace nada si el dueño ya tiene algún corte (RLS ya scopea el conteo a su negocio). */
	@Transactional
	public void sembrarSiHaceFalta(UUID duenoId) {
		if (corteRepository.count() > 0) {
			return;
		}
		for (CorteDeEjemplo ejemplo : CATALOGO_DE_EJEMPLO) {
			corteRepository.save(new CorteEntity(
					ejemplo.nombre(), ejemplo.plu(), ejemplo.cuarto(), ejemplo.zonaMapa(), true, duenoId));
		}
		// Flush explícito: sin esto, los INSERT quedan en el buffer de Hibernate hasta que
		// algo más dispare un flush automático — si para entonces la sesión ya cambió de
		// identidad (ej. un test que sigue con otro usuario en la misma transacción), ese
		// flush corre con las claims de RLS equivocadas y el INSERT se rechaza.
		corteRepository.flush();
	}
}
