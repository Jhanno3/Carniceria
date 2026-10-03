package com.carniceria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// order = HIGHEST_PRECEDENCE: el interceptor transaccional debe envolver POR FUERA de
// RlsSessionAspect, para que la transacción ya esté abierta cuando el aspecto hace
// SET LOCAL sobre la conexión (shared.security.RlsSessionAspect).
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
