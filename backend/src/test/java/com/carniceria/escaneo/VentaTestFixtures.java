package com.carniceria.escaneo;

import com.carniceria.escaneo.entity.VentaEntity;
import com.carniceria.escaneo.repository.VentaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solo para tests de este paquete: inserta una venta directo por repositorio (no vía
 * POST /ventas), para poder fabricar una con una fecha_hora pasada que un escaneo normal
 * no permite (siempre usa Instant.now()) — necesario para probar la ventana de 5 minutos
 * de FR-308 sin esperarla de verdad.
 *
 * Tiene que ser un método @Transactional de un bean DISTINTO del test (no un método
 * privado del test, ni un repositorio llamado directo): RlsSessionAspect solo propaga el
 * JWT antes de la ejecución de un método anotado @Transactional, y esa anotación hay que
 * verla a través de un proxy de Spring real — llamar directo a corteRepository/ventaRepository
 * desde el cuerpo del test, inmediatamente después de jwtClaimsHolder.set(...), no alcanza
 * a disparar el aspecto de forma confiable (mismo motivo, en espíritu, que el hallazgo de
 * flush diferido documentado en CatalogoInicialService/CorteService).
 */
@Component
public class VentaTestFixtures {

	private final VentaRepository ventaRepository;

	public VentaTestFixtures(VentaRepository ventaRepository) {
		this.ventaRepository = ventaRepository;
	}

	@Transactional
	public UUID crearVentaDirecta(
			UUID corteId, UUID usuarioId, UUID duenoId, String codigoLeido, Instant fechaHora) {
		VentaEntity venta = new VentaEntity(
				corteId, new BigDecimal("1.250"), null, codigoLeido, UUID.randomUUID(), usuarioId, duenoId, fechaHora);
		venta = ventaRepository.save(venta);
		ventaRepository.flush();
		return venta.getId();
	}
}
