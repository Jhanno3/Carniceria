package com.carniceria.shared.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
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

	@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
	private String jwkSetUri;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtClaimsHolder jwtClaimsHolder)
			throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
				.addFilterAfter(new JwtClaimsContextFilter(jwtClaimsHolder), BearerTokenAuthenticationFilter.class);
		return http.build();
	}

	/**
	 * Los proyectos nuevos de Supabase firman con una clave asimétrica ES256 (JWKS con
	 * {@code "kty":"EC"}), no con el HS256 de clave compartida de proyectos viejos. El
	 * decoder que Spring Boot auto-configura a partir de {@code jwk-set-uri} sin más
	 * ajuste solo acepta RS256 por defecto, así que rechaza cualquier JWT real de Supabase
	 * con "Another algorithm expected, or no matching key(s) found" (research.md).
	 */
	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
				.jwsAlgorithm(SignatureAlgorithm.ES256)
				.build();
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
