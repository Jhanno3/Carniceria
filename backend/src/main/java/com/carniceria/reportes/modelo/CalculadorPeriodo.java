package com.carniceria.reportes.modelo;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Dominio puro — sin Spring ni JPA (plan-fase4.md, 3.4). FR-402: agrupar el reporte de
 * rendimiento por período, a elección del dueño.
 */
public final class CalculadorPeriodo {

	private CalculadorPeriodo() {
	}

	public enum Periodo {
		dia, semana, mes
	}

	/** Semana argentina de lunes a domingo (constitution.md, Principio V). */
	public static LocalDate inicioDelBucket(LocalDate fecha, Periodo periodo) {
		return switch (periodo) {
			case dia -> fecha;
			case semana -> fecha.with(DayOfWeek.MONDAY);
			case mes -> fecha.withDayOfMonth(1);
		};
	}
}
