package com.carniceria.escaneo.repository;

import com.carniceria.escaneo.entity.StockPorCorteEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<StockPorCorteEntity, UUID> {
}
