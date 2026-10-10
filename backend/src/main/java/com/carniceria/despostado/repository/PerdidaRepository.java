package com.carniceria.despostado.repository;

import com.carniceria.despostado.entity.PerdidaEntity;
import com.carniceria.despostado.modelo.RegistroPerdidaHistorico;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PerdidaRepository extends JpaRepository<PerdidaEntity, UUID> {

	List<PerdidaEntity> findByMediaResId(UUID mediaResId);

	void deleteByMediaResId(UUID mediaResId);

	// Historial para que "Automático" (Despostado) también prellene Hueso/Grasa/Merma, no
	// solo los cortes (ver DespostadoRepository.buscarHistoricoPorUsuario). Mismo motivo
	// para excluir los tipoEntrada de "Añadir stock" (Fase 7): esas entradas siempre
	// guardan sus tres pérdidas en 0 (no hay desposte real), así que mezclarlas diluiría
	// el promedio real hacia abajo sin ningún significado.
	@Query("select new com.carniceria.despostado.modelo.RegistroPerdidaHistorico(p.tipo, p.kg, m.pesoKg) "
			+ "from PerdidaEntity p, MediaResEntity m "
			+ "where m.id = p.mediaResId and m.creadoPor = :usuarioId "
			+ "and m.tipoEntrada not in ("
			+ "com.carniceria.despostado.entity.MediaResEntity.TipoEntrada.AchurasEmbutidos, "
			+ "com.carniceria.despostado.entity.MediaResEntity.TipoEntrada.Cerdo, "
			+ "com.carniceria.despostado.entity.MediaResEntity.TipoEntrada.Carne)")
	List<RegistroPerdidaHistorico> buscarHistoricoPorUsuario(UUID usuarioId);
}
