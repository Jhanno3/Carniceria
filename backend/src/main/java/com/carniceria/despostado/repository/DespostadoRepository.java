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

	// Historial para la estimación automática (data-model.md, "Carga atómica y estimación
	// automática"): un registro por fila de despostado, con el peso de entrada de su
	// propia media res, acotado a las entradas cargadas por el mismo usuario (cada
	// carnicero suele trabajar siempre con la misma raza/proveedor, así que su propio
	// historial estima mejor que el promedio de todos los usuarios). Sin relación
	// @ManyToOne a propósito (cada entidad es independiente, ver research.md) — es un
	// cruce explícito por igualdad de id.
	@Query("select new com.carniceria.despostado.modelo.RegistroHistorico(d.corteId, d.kg, m.pesoKg) "
			+ "from DespostadoEntity d, MediaResEntity m "
			+ "where m.id = d.mediaResId and m.creadoPor = :usuarioId")
	List<RegistroHistorico> buscarHistoricoPorUsuario(UUID usuarioId);
}
