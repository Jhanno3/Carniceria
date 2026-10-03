package com.carniceria.despostado.repository;

import com.carniceria.despostado.entity.MediaResEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaResRepository extends JpaRepository<MediaResEntity, UUID> {

	List<MediaResEntity> findByCreadoEnBetweenOrderByCreadoEnDesc(Instant desde, Instant hasta);
}
