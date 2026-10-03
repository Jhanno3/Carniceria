package com.carniceria.despostado.repository;

import com.carniceria.despostado.entity.PerdidaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerdidaRepository extends JpaRepository<PerdidaEntity, UUID> {

	List<PerdidaEntity> findByMediaResId(UUID mediaResId);

	void deleteByMediaResId(UUID mediaResId);
}
