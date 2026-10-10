package com.carniceria.despostado.repository;

import com.carniceria.despostado.entity.DespostadoEntity;
import com.carniceria.despostado.modelo.IngresoCortesPorMediaRes;
import com.carniceria.despostado.modelo.RegistroHistorico;
import com.carniceria.despostado.modelo.VendibleKgPorMediaRes;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DespostadoRepository extends JpaRepository<DespostadoEntity, UUID> {

	List<DespostadoEntity> findByMediaResId(UUID mediaResId);

	void deleteByMediaResId(UUID mediaResId);

	// Fase 4 (plan-fase4.md, 3.5): el vendibleKg de cada media res, agregado en una sola
	// consulta (group by sobre una sola tabla, sin el riesgo de producto cartesiano que
	// tuvo V15__stock_por_corte.sql al unir dos tablas a la vez). Evita instanciar
	// ResumenDespostado (que necesita Perdidas, que acá no hace falta para nada) por cada
	// media res del rango de un reporte.
	@Query("select new com.carniceria.despostado.modelo.VendibleKgPorMediaRes(d.mediaResId, sum(d.kg)) "
			+ "from DespostadoEntity d where d.mediaResId in :mediaResIds group by d.mediaResId")
	List<VendibleKgPorMediaRes> sumarVendibleKgPorMediaRes(@Param("mediaResIds") Collection<UUID> mediaResIds);

	// Historial para la estimación automática (data-model.md, "Carga atómica y estimación
	// automática"): un registro por fila de despostado, con el peso de entrada de su
	// propia media res, acotado a las entradas cargadas por el mismo usuario (cada
	// carnicero suele trabajar siempre con la misma raza/proveedor, así que su propio
	// historial estima mejor que el promedio de todos los usuarios). Sin relación
	// @ManyToOne a propósito (cada entidad es independiente, ver research.md) — es un
	// cruce explícito por igualdad de id.
	//
	// tipoProducto = Vacuno (Fase 7): el modal "Añadir stock" (Achuras/Embutidos, Cerdo,
	// Carne) también crea filas de despostado, pero como esas entradas son degeneradas
	// (kg = pesoKg de la media res, siempre 100 %), si entraran acá contaminarían el
	// promedio histórico de esos cortes puntuales y la estimación los proyectaría como si
	// fueran ~100 % de cualquier media res nueva — de ahí el "sobran X kg" aunque la tabla
	// de Cortes vendibles (que ya solo muestra Vacuno) estuviera bien cargada.
	@Query("select new com.carniceria.despostado.modelo.RegistroHistorico(d.corteId, d.kg, m.pesoKg) "
			+ "from DespostadoEntity d, MediaResEntity m, CorteEntity c "
			+ "where m.id = d.mediaResId and m.creadoPor = :usuarioId and c.id = d.corteId "
			+ "and c.tipoProducto = com.carniceria.cortes.entity.CorteEntity.TipoProducto.Vacuno")
	List<RegistroHistorico> buscarHistoricoPorUsuario(UUID usuarioId);

	// Fase 5 (FR-505, "Beneficio por kg vendible"): mismo estilo sin @ManyToOne que
	// buscarHistoricoPorUsuario — cruce explícito por igualdad, no por relación JPA. Solo
	// suma los cortes con precioVenta cargado (el `where` los filtra antes del group by):
	// un corte sin precio no entra ni al numerador ni al denominador del promedio ponderado
	// que arma AgregadorRendimiento, en vez de contarlo como precio 0.
	@Query("select new com.carniceria.despostado.modelo.IngresoCortesPorMediaRes("
			+ "d.mediaResId, sum(d.kg), sum(d.kg * c.precioVenta)) "
			+ "from DespostadoEntity d, CorteEntity c "
			+ "where c.id = d.corteId and c.precioVenta is not null and d.mediaResId in :mediaResIds "
			+ "group by d.mediaResId")
	List<IngresoCortesPorMediaRes> sumarKgYValorVentaPorMediaRes(@Param("mediaResIds") Collection<UUID> mediaResIds);
}
