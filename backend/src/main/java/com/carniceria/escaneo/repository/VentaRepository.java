package com.carniceria.escaneo.repository;

import com.carniceria.escaneo.entity.VentaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VentaRepository extends JpaRepository<VentaEntity, UUID> {

	Optional<VentaEntity> findByIdClienteLocal(UUID idClienteLocal);

	List<VentaEntity> findByFechaHoraBetweenOrderByFechaHoraDesc(Instant desde, Instant hasta);

	// UPDATE explícito (no mutar la entidad): mismo motivo que ConfigEtiquetaRepository.actualizar
	// — si RLS bloquea el UPDATE (empleado sin permiso o fuera de la ventana de 5 minutos),
	// tiene que notarse como 0 filas afectadas, no como un 200 silencioso sin haber cambiado nada.
	@Modifying(clearAutomatically = true)
	@Query("update VentaEntity v set v.anulada = true where v.id = :id")
	int anular(@Param("id") UUID id);
}
