package com.carniceria.cortes.service;

import com.carniceria.cortes.entity.CorteEntity;
import com.carniceria.cortes.entity.CorteEntity.Cuarto;
import com.carniceria.cortes.entity.CorteEntity.TipoProducto;
import com.carniceria.cortes.repository.CorteRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra el catálogo de ejemplo (especificacion-carniceria.md, sección 2.2, más los 7
 * cortes agregados en V21, los 4 reactivados en V22 y los 16 de Achuras/Embutidos/Cerdo de
 * V29, con Rabo y Carne picada reclasificados a "Carne" en V30 — ningún corte queda afuera
 * ya) para un dueño recién aprobado, así no arranca con la
 * pantalla de Despostado vacía. Se llama desde {@code PerfilService} bajo la sesión del
 * propio dueño (RLS exige {@code dueno_id = mi_negocio_id()}; sembrar "para otro" desde una
 * sesión de admin no pasaría esa política — ver V10__multi_negocio.sql).
 */
@Service
public class CatalogoInicialService {

	private record CorteDeEjemplo(String nombre, int plu, Cuarto cuarto, TipoProducto tipoProducto, String zonaMapa) {
	}

	private static final List<CorteDeEjemplo> CATALOGO_DE_EJEMPLO = List.of(
			new CorteDeEjemplo("Cuadril", 1, Cuarto.Trasero, TipoProducto.Vacuno, "cuadril"),
			new CorteDeEjemplo("Lomo", 2, Cuarto.Trasero, TipoProducto.Vacuno, "lomo"),
			new CorteDeEjemplo("Bife angosto", 3, Cuarto.Trasero, TipoProducto.Vacuno, "bifeAngosto"),
			new CorteDeEjemplo("Nalga", 4, Cuarto.Trasero, TipoProducto.Vacuno, "nalga"),
			new CorteDeEjemplo("Cuadrada", 5, Cuarto.Trasero, TipoProducto.Vacuno, "nalga"),
			new CorteDeEjemplo("Tapa de nalga", 6, Cuarto.Trasero, TipoProducto.Vacuno, "nalga"),
			new CorteDeEjemplo("Bola de lomo", 7, Cuarto.Trasero, TipoProducto.Vacuno, "bola"),
			new CorteDeEjemplo("Peceto", 8, Cuarto.Trasero, TipoProducto.Vacuno, "bola"),
			new CorteDeEjemplo("Vacío", 12, Cuarto.Trasero, TipoProducto.Vacuno, "vacio"),
			new CorteDeEjemplo("Matambre", 9, Cuarto.Trasero, TipoProducto.Vacuno, "matambre"),
			new CorteDeEjemplo("Osobuco", 10, Cuarto.Ambos, TipoProducto.Vacuno, "osobuco"),
			new CorteDeEjemplo("Asado", 11, Cuarto.Delantero, TipoProducto.Vacuno, "asado"),
			new CorteDeEjemplo("Tapa de asado", 13, Cuarto.Delantero, TipoProducto.Vacuno, "asado"),
			new CorteDeEjemplo("Falda", 14, Cuarto.Delantero, TipoProducto.Vacuno, "falda"),
			new CorteDeEjemplo("Bife ancho", 15, Cuarto.Delantero, TipoProducto.Vacuno, "bifeAncho"),
			new CorteDeEjemplo("Paleta", 17, Cuarto.Delantero, TipoProducto.Vacuno, "paleta"),
			// Reclasificado a "Carne" en Fase 7 (ajuste pedido por chat, V30): no se desposta
			// de una media res tampoco, aunque sea carne vacuna — mismo criterio que Rabo.
			new CorteDeEjemplo("Carne picada / recortes", 21, null, TipoProducto.Carne, null),
			// Agregados en V21__cortes_nuevos.sql — mismos PLU que esa migración usó para
			// los negocios que ya existían (16/18/19/20 quedaron libres al sacar Aguja/
			// Marucha/Pecho/Cogote en V8; 22/23/24 son nuevos).
			new CorteDeEjemplo("Espinazo", 16, Cuarto.Ambos, TipoProducto.Vacuno, "espinazo"),
			new CorteDeEjemplo("Roast beef", 18, Cuarto.Delantero, TipoProducto.Vacuno, "roastBeef"),
			new CorteDeEjemplo("Chingolo", 19, Cuarto.Delantero, TipoProducto.Vacuno, "chingolo"),
			new CorteDeEjemplo("Tortuga", 20, Cuarto.Trasero, TipoProducto.Vacuno, "tortuga"),
			new CorteDeEjemplo("Colita de cuadril", 22, Cuarto.Trasero, TipoProducto.Vacuno, "colitaCuadril"),
			new CorteDeEjemplo("Entraña", 23, Cuarto.Delantero, TipoProducto.Vacuno, "entrana"),
			new CorteDeEjemplo("Asado americano", 24, Cuarto.Delantero, TipoProducto.Vacuno, "asadoAmericano"),
			// Reactivados en V22__reactivar_cortes_redundantes.sql — PLU nuevos (25-28, no
			// los originales 16/18/19/20, ya ocupados arriba), cada uno con su propia zona
			// de mapa esta vez (antes Marucha compartía con Paleta).
			new CorteDeEjemplo("Aguja", 25, Cuarto.Delantero, TipoProducto.Vacuno, "aguja"),
			new CorteDeEjemplo("Marucha", 26, Cuarto.Delantero, TipoProducto.Vacuno, "marucha"),
			new CorteDeEjemplo("Pecho", 27, Cuarto.Delantero, TipoProducto.Vacuno, "pecho"),
			new CorteDeEjemplo("Cogote", 28, Cuarto.Delantero, TipoProducto.Vacuno, "cogote"),
			// Achuras y Embutidos / Cerdo (Fase 7, V29__seed_achuras_embutidos_cerdo.sql):
			// no se despostan de una media res, así que no tienen cuarto ni zona de mapa.
			new CorteDeEjemplo("Chinchulín", 29, null, TipoProducto.AchurasEmbutidos, null),
			new CorteDeEjemplo("Molleja", 30, null, TipoProducto.AchurasEmbutidos, null),
			new CorteDeEjemplo("Mondongo", 32, null, TipoProducto.AchurasEmbutidos, null),
			new CorteDeEjemplo("Lengua", 33, null, TipoProducto.AchurasEmbutidos, null),
			new CorteDeEjemplo("Riñón", 34, null, TipoProducto.AchurasEmbutidos, null),
			new CorteDeEjemplo("Corazón", 35, null, TipoProducto.AchurasEmbutidos, null),
			new CorteDeEjemplo("Hígado", 36, null, TipoProducto.AchurasEmbutidos, null),
			new CorteDeEjemplo("Fresca", 37, null, TipoProducto.AchurasEmbutidos, null),
			new CorteDeEjemplo("Morcilla Vasca", 38, null, TipoProducto.AchurasEmbutidos, null),
			new CorteDeEjemplo("Morcilla", 39, null, TipoProducto.AchurasEmbutidos, null),
			new CorteDeEjemplo("Chorizo", 40, null, TipoProducto.AchurasEmbutidos, null),
			new CorteDeEjemplo("Bondiola", 41, null, TipoProducto.Cerdo, null),
			new CorteDeEjemplo("Pechito", 42, null, TipoProducto.Cerdo, null),
			new CorteDeEjemplo("Carré", 43, null, TipoProducto.Cerdo, null),
			new CorteDeEjemplo("Cordero", 44, null, TipoProducto.Cerdo, null),
			// "Carne" (ajuste pedido por chat, V30): Rabo es carne vacuna pero no se desposta
			// de la media res — mismo PLU 31 de siempre, solo cambia su tipoProducto.
			new CorteDeEjemplo("Rabo", 31, null, TipoProducto.Carne, null));

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
					ejemplo.nombre(), ejemplo.plu(), ejemplo.cuarto(), ejemplo.tipoProducto(), ejemplo.zonaMapa(),
					true, null, duenoId, null));
		}
		// Flush explícito: sin esto, los INSERT quedan en el buffer de Hibernate hasta que
		// algo más dispare un flush automático — si para entonces la sesión ya cambió de
		// identidad (ej. un test que sigue con otro usuario en la misma transacción), ese
		// flush corre con las claims de RLS equivocadas y el INSERT se rechaza.
		corteRepository.flush();
	}
}
