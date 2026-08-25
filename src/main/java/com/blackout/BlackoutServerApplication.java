package com.blackout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot composition root for the BLACKOUT relay (REST + JPA + H2).
 *
 * Normally started embedded inside the JavaFX process by
 * {@link com.blackout.config.BackendRuntime}; it can also be booted standalone in
 * headless mode for API-only operations:
 *
 * <pre>mvn spring-boot:run -Dspring-boot.run.main-class=com.blackout.BlackoutServerApplication</pre>
 */
@SpringBootApplication
public class BlackoutServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlackoutServerApplication.class, args);
    }
}
