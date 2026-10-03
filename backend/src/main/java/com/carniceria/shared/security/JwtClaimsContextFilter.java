package com.carniceria.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Después de que Spring Security valida el JWT, toma sus claims (solo las que Postgres
 * necesita: sub, role, email) y las deja en JwtClaimsHolder para el resto de la request.
 * RlsSessionAspect las propaga a la conexión con SET LOCAL.
 */
public class JwtClaimsContextFilter extends OncePerRequestFilter {

	private final JwtClaimsHolder jwtClaimsHolder;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public JwtClaimsContextFilter(JwtClaimsHolder jwtClaimsHolder) {
		this.jwtClaimsHolder = jwtClaimsHolder;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication instanceof JwtAuthenticationToken jwtAuth) {
				jwtClaimsHolder.set(toClaimsJson(jwtAuth.getToken()));
			}
			filterChain.doFilter(request, response);
		} finally {
			jwtClaimsHolder.clear();
		}
	}

	private String toClaimsJson(Jwt jwt) throws IOException {
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("sub", jwt.getSubject());
		if (jwt.getClaimAsString("role") != null) {
			claims.put("role", jwt.getClaimAsString("role"));
		}
		if (jwt.getClaimAsString("email") != null) {
			claims.put("email", jwt.getClaimAsString("email"));
		}
		return objectMapper.writeValueAsString(claims);
	}
}
