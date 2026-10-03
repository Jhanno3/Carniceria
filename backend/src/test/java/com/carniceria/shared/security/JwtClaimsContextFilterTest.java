package com.carniceria.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtClaimsContextFilterTest {

	private final JwtClaimsHolder jwtClaimsHolder = new JwtClaimsHolder();
	private final JwtClaimsContextFilter filter = new JwtClaimsContextFilter(jwtClaimsHolder);

	@AfterEach
	void limpiarContexto() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void conJwtAutenticado_dejaLasClaimsEnElHolderMientrasCorreLaCadena() throws Exception {
		Jwt jwt = Jwt.withTokenValue("token-de-prueba")
				.header("alg", "none")
				.claim("sub", "87b585e4-f4e8-4d9a-858d-efb77058a49d")
				.claim("role", "authenticated")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(3600))
				.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = (req, res) -> {
			assertThat(jwtClaimsHolder.get())
					.contains("87b585e4-f4e8-4d9a-858d-efb77058a49d")
					.contains("authenticated");
		};

		filter.doFilter(request, response, chain);

		assertThat(jwtClaimsHolder.get()).isNull();
	}

	@Test
	void sinAutenticacion_laCadenaCorreSinClaims() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
		assertThat(jwtClaimsHolder.get()).isNull();
	}
}
