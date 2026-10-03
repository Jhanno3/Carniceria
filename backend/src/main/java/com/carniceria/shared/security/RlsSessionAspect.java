package com.carniceria.shared.security;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Antes de que corra cualquier método @Transactional de un service, deja el JWT del
 * usuario como variable de sesión de Postgres (SET LOCAL), para que auth.uid() y las
 * políticas de RLS lo vean como si la query viniera directo de Supabase (research.md,
 * "Propagación del JWT a Postgres para que RLS funcione").
 *
 * @Order: debe ejecutarse DESPUÉS de que el interceptor transaccional abrió la
 * transacción (ver @EnableTransactionManagement en BackendApplication), para que el
 * SET LOCAL caiga dentro de la misma transacción que las queries del método.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RlsSessionAspect {

	private final EntityManager entityManager;
	private final JwtClaimsHolder jwtClaimsHolder;

	public RlsSessionAspect(EntityManager entityManager, JwtClaimsHolder jwtClaimsHolder) {
		this.entityManager = entityManager;
		this.jwtClaimsHolder = jwtClaimsHolder;
	}

	@Before("@annotation(org.springframework.transaction.annotation.Transactional)")
	public void propagarClaims() {
		String claimsJson = jwtClaimsHolder.get();
		if (claimsJson == null) {
			return;
		}
		Session session = entityManager.unwrap(Session.class);
		session.doWork(connection -> {
			try (var statement = connection.prepareStatement("select set_config('request.jwt.claims', ?, true)")) {
				statement.setString(1, claimsJson);
				statement.execute();
			}
		});
	}
}
