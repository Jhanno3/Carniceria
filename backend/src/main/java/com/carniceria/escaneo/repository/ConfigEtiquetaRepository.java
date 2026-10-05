package com.carniceria.escaneo.repository;

import com.carniceria.escaneo.entity.ConfigEtiquetaEntity;
import com.carniceria.escaneo.modelo.ConfigEtiqueta;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConfigEtiquetaRepository extends JpaRepository<ConfigEtiquetaEntity, UUID> {

	// UPDATE explícito (no mutar la entidad): mismo motivo que CorteRepository.actualizar
	// — si RLS bloquea el UPDATE, Hibernate tiene que enterarse por las filas afectadas,
	// no devolver un 200 silencioso sin haber cambiado nada.
	@Modifying(clearAutomatically = true)
	@Query("update ConfigEtiquetaEntity c set c.prefijoDesde = :prefijoDesde, c.prefijoHasta = :prefijoHasta, "
			+ "c.inicioPlu = :inicioPlu, c.largoPlu = :largoPlu, c.inicioValor = :inicioValor, "
			+ "c.largoValor = :largoValor, c.tipoValor = :tipoValor, c.decimales = :decimales "
			+ "where c.duenoId = :duenoId")
	int actualizar(@Param("duenoId") UUID duenoId, @Param("prefijoDesde") int prefijoDesde,
			@Param("prefijoHasta") int prefijoHasta, @Param("inicioPlu") int inicioPlu,
			@Param("largoPlu") int largoPlu, @Param("inicioValor") int inicioValor,
			@Param("largoValor") int largoValor, @Param("tipoValor") ConfigEtiqueta.TipoValor tipoValor,
			@Param("decimales") int decimales);
}
