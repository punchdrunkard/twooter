package xyz.twooter.auth.infrastructure.jwt;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.twooter.auth.domain.TokenType;

class JWTUtilTest {

	private final String SECRET_KEY = "testSecretKeytestSecretKeytestSecretKeytestSecretKey";
	private JWTUtil jwtUtil = new JWTUtil(SECRET_KEY);

	@Test
	@DisplayName("액세스 토큰 생성 및 검증")
	void createAndValidateAccessToken() {
		// given
		String handle = "testUser";
		Long expiration = 3600000L; // 1시간

		// when
		String token = jwtUtil.createJwt(handle, TokenType.ACCESS, expiration);

		// then
		assertThat(token).isNotNull();
		assertThat(jwtUtil.isExpired(token)).isFalse();
		assertThat(jwtUtil.getHandle(token)).isEqualTo(handle);
	}

	@Test
	@DisplayName("만료된 토큰 테스트")
	void expiredToken() {
		// given
		String handle = "testUser";
		Long expiration = -10000L; // 음수 값으로 이미 만료된 토큰 생성

		// when
		String token = jwtUtil.createJwt(handle, TokenType.ACCESS, expiration);

		// then
		assertThat(jwtUtil.isExpired(token)).isTrue();
	}

}
