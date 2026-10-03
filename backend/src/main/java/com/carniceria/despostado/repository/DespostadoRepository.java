package com.carniceria.despostado.repository;

import com.carniceria.despostado.entity.DespostadoEntity;
import com.carniceria.despostado.modelo.RegistroHistorico;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DespostadoRepository extends JpaRepository<DespostadoEntity, UUID> {

	List<DespostadoEntity> findByMediaResId(UUID mediaResId);

	void deleteByMediaResId(UUID mediaResId);

	// Historial completo para la estimación automática (data-model.md, "Carga atómica y
	// estimación automática"): un registro por fila de despostado, con el peso de
	// entrada de su propia media res. Sin relación @ManyToOne a propósito (cada entidad
	// es independiente, ver research.md) — es un cruce explícito por igualdad de id.
	@Query("select new com.carniceria.despostado.modelo.RegistroHistorico(d.corteId, d.kg, m.pesoKg) "
			+ "from DespostadoEntity d, MediaResEntity m where m.id = d.mediaResId")
	List<RegistroHistorico> buscarHistoricoCompleto();
}
