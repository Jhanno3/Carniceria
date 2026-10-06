package com.carniceria.cortes.repository;

import com.carniceria.cortes.entity.CorteEntity;
import com.carniceria.cortes.entity.CorteEntity.Cuarto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CorteRepository extends JpaRepository<CorteEntity, UUID> {

	Optional<CorteEntity> findByPlu(Integer plu);

	List<CorteEntity> findByActivoTrueOrderByNombreAsc();

	List<CorteEntity> findAllByOrderByNombreAsc();

	// UPDATE explícito, no mutar la entidad y confiar en el flush: un empleado puede
	// LEER un corte activo (política de RLS de solo lectura) pero no escribirlo: sin
	// esto, Hibernate no se entera de que RLS bloqueó el UPDATE y devuelve éxito
	// aunque no haya cambiado nada en la base (mismo hallazgo que en PerfilRepository).
	// clearAutomatically: sin esto, el findById() posterior puede devolver la entidad
	// vieja que ya estaba en la sesión (ej. por el chequeo de PLU duplicado), aunque
	// el UPDATE directo ya haya cambiado la fila en la base.
	@Modifying(clearAutomatically = true)
	@Query("update CorteEntity c set c.nombre = :nombre, c.plu = :plu, c.cuarto = :cuarto, "
			+ "c.zonaMapa = :zonaMapa, c.activo = :activo, c.precioVenta = :precioVenta where c.id = :id")
	int actualizar(@Param("id") UUID id, @Param("nombre") String nombre, @Param("plu") Integer plu,
			@Param("cuarto") Cuarto cuarto, @Param("zonaMapa") String zonaMapa, @Param("activo") boolean activo,
			@Param("precioVenta") BigDecimal precioVenta);
}
