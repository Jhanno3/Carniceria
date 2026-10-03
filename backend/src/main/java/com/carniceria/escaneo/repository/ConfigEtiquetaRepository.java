package com.carniceria.escaneo.repository;

import com.carniceria.escaneo.entity.ConfigEtiquetaEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigEtiquetaRepository extends JpaRepository<ConfigEtiquetaEntity, UUID> {
}
