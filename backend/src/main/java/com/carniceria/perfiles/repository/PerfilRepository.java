package com.carniceria.perfiles.repository;

import com.carniceria.perfiles.entity.PerfilEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PerfilRepository extends JpaRepository<PerfilEntity, UUID> {

	List<PerfilEntity> findByEstadoOrderByNombreAsc(PerfilEntity.Estado estado);

	// UPDATE explícito (no mutar la entidad administrada por Hibernate y confiar en el
	// flush): PerfilEntity no tiene @Version, así que si RLS bloquea la escritura
	// (porque quien llama puede LEER su propia fila pero no escribirla), Hibernate no
	// se entera y no tira ningún error — hace como que guardó y no guardó nada. Un
	// UPDATE con @Modifying sí devuelve cuántas filas tocó de verdad.
	// clearAutomatically: sin esto, un findById() posterior puede devolver la entidad
	// vieja que ya estaba en la sesión de Hibernate (ej. por el listado previo),
	// aunque el UPDATE directo ya haya cambiado la fila en la base.
	@Modifying(clearAutomatically = true)
	@Query("update PerfilEntity p set p.rol = :rol, p.estado = :estado where p.id = :id")
	int actualizarRolYEstado(@Param("id") UUID id, @Param("rol") PerfilEntity.Rol rol,
			@Param("estado") PerfilEntity.Estado estado);
}
