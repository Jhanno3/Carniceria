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

	// Variante que además fija dueno_id (V10__multi_negocio.sql): se usa al promover a
	// "dueno" (dueno_id = su propio id) o a "admin" (dueno_id = null). Para "empleado" no
	// hace falta: su dueno_id ya quedó fijado al registrarse con el código de invitación
	// (ver PerfilService.crearDesdeMetadata) y esta variante no lo toca.
	@Modifying(clearAutomatically = true)
	@Query("update PerfilEntity p set p.rol = :rol, p.estado = :estado, p.duenoId = :duenoId where p.id = :id")
	int actualizarRolEstadoYDuenoId(@Param("id") UUID id, @Param("rol") PerfilEntity.Rol rol,
			@Param("estado") PerfilEntity.Estado estado, @Param("duenoId") UUID duenoId);

	// security definer (V10__multi_negocio.sql): valida un código de invitación (el id de
	// un dueño aprobado) sin pasar por perfiles_select_propio, que le escondería la fila a
	// quien todavía no tiene ningún perfil propio.
	@Query(value = "select es_dueno_valido(:candidato)", nativeQuery = true)
	boolean esDuenoValido(@Param("candidato") UUID candidato);
}
