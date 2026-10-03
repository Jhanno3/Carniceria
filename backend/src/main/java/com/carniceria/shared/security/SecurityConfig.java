package com.carniceria.shared.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * La API es un resource server sin estado: todo pedido trae su JWT de Supabase Auth.
 * La autorización fina (quién ve qué) no se decide acá, la decide RLS en Postgres
 * (constitution.md, Principio II) — esta capa solo exige "JWT válido" y después deja
 * que JwtClaimsContextFilter lo propague.
 */
@Configuration
public class SecurityConfig {

	@Value("${app.cors-allowed-origins}")
	private String corsAllowedOrigins;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtClaimsHolder jwtClaimsHolder)
			throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
				}))
				.addFilterAfter(new JwtClaimsContextFilter(jwtClaimsHolder), BearerTokenAuthenticationFilter.class);
		return http.build();
	}

	private CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuracion = new CorsConfiguration();
		configuracion.setAllowedOrigins(List.of(corsAllowedOrigins.split(",")));
		configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));

		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", configuracion);
		return fuente;
	}
}
