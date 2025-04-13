package xyz.twooter.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import xyz.twooter.auth.infrastructure.jwt.JWTUtil;

@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfiguration {

	private static final String TEST_JWT_SECRET =
		"testSecretKeyForJWTGenerationInTestEnvironmentMustBeSecureLongEnoughToWorkWithHS256Algorithm";

	@Bean
	@Primary
	public JWTUtil jwtUtil() {
		return new JWTUtil(TEST_JWT_SECRET);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
			.build();
	}
}
