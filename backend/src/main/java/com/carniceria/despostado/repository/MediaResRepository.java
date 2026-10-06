package com.carniceria.despostado.repository;

import com.carniceria.despostado.entity.MediaResEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaResRepository extends JpaRepository<MediaResEntity, UUID> {

	List<MediaResEntity> findByCreadoEnBetweenOrderByCreadoEnDesc(Instant desde, Instant hasta);

	// Fase 4 (plan-fase4.md, 3.1): los reportes filtran por fecha de negocio, no por
	// creado_en (el timestamp de auditoría que usa listar() de Fase 1) — evita construir
	// instantes de inicio/fin de día en zona horaria para un reporte que puede cubrir
	// semanas o meses.
	List<MediaResEntity> findByFechaBetween(LocalDate desde, LocalDate hasta);
}
