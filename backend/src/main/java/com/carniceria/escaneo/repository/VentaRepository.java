package com.carniceria.escaneo.repository;

import com.carniceria.escaneo.entity.VentaEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaRepository extends JpaRepository<VentaEntity, UUID> {

	Optional<VentaEntity> findByIdClienteLocal(UUID idClienteLocal);

	List<VentaEntity> findByFechaHoraBetweenOrderByFechaHoraDesc(Instant desde, Instant hasta);
}
