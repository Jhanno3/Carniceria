package com.carniceria.shared.security;

import org.springframework.stereotype.Component;

/**
 * Guarda, para el hilo de la request actual, el JSON de claims del JWT ya validado por
 * Spring Security. RlsSessionAspect lo lee para propagarlo a Postgres (SET LOCAL) y que
 * las políticas de RLS se apliquen por usuario real.
 */
@Component
public class JwtClaimsHolder {

	private final ThreadLocal<String> claimsJson = new ThreadLocal<>();

	public void set(String json) {
		claimsJson.set(json);
	}

	public String get() {
		return claimsJson.get();
	}

	public void clear() {
		claimsJson.remove();
	}
}
