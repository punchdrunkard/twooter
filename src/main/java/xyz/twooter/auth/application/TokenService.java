package xyz.twooter.auth.application;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.twooter.auth.domain.RefreshToken;
import xyz.twooter.auth.domain.TokenType;
import xyz.twooter.auth.domain.exception.InvalidTokenException;
import xyz.twooter.auth.infrastructure.jwt.JWTUtil;
import xyz.twooter.common.infrastructure.redis.RedisUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

	// Redis 키 접두어
	private static final String TOKEN_PREFIX = "refresh:token:";
	private static final String USER_PREFIX = "refresh:user:";
	private static final String BLACKLIST_PREFIX = "%s:blacklist:"; // %s에 TokenType이 들어감
	private static final int REVOKED_TOKEN_RETENTION_SECONDS = 300; // 5분
	private final RedisUtil redisUtil;
	private final JWTUtil jwtUtil;
	@Value("${spring.jwt.refresh-token-validity}")
	private Long refreshTokenValidityMs;

	/**
	 * 토큰을 블랙리스트에 추가합니다.
	 *
	 * @param token     블랙리스트에 추가할 토큰
	 * @param tokenType 토큰 타입 (ACCESS 또는 REFRESH)
	 */
	public void blacklistToken(String token, TokenType tokenType) {
		try {
			String blacklistKey = String.format(BLACKLIST_PREFIX, tokenType.name().toLowerCase()) + token;

			if (tokenType == TokenType.ACCESS) {
				// 액세스 토큰의 경우 남은 유효기간 동안만 블랙리스트에 유지
				long expiryInMs = jwtUtil.getRemainingTimeInMillis(token);
				if (!jwtUtil.isExpired(token)) {
					redisUtil.set(blacklistKey, "true", expiryInMs, TimeUnit.MILLISECONDS);
					log.info("Added {} token to blacklist", tokenType);
				}
			} else {
				// 리프레시 토큰의 경우 전체 유효기간 동안 블랙리스트에 유지
				redisUtil.set(blacklistKey, LocalDateTime.now().toString(), refreshTokenValidityMs,
					TimeUnit.MILLISECONDS);
				log.info("Added {} token to blacklist: {}", tokenType, token);
			}
		} catch (Exception e) {
			log.error("Error adding {} token to blacklist: {}", tokenType, e.getMessage());
		}
	}

	/**
	 * 토큰이 블랙리스트에 있는지 확인합니다.
	 *
	 * @param token     확인할 토큰
	 * @param tokenType 토큰 타입 (ACCESS 또는 REFRESH)
	 * @return 블랙리스트에 있으면 true, 없으면 false
	 */
	public boolean isTokenBlacklisted(String token, TokenType tokenType) {
		String blacklistKey = String.format(BLACKLIST_PREFIX, tokenType.name().toLowerCase()) + token;
		return redisUtil.hasKey(blacklistKey);
	}

	/**
	 * 새로운 Refresh Token을 생성하고 Redis에 저장합니다.
	 *
	 * @param handle 사용자 핸들
	 * @return 생성된 Refresh 토큰
	 */
	public String createRefreshToken(String handle) {
		// JWT 토큰 생성
		String tokenValue = jwtUtil.createJwt(handle, TokenType.REFRESH, refreshTokenValidityMs);
		LocalDateTime expiryDate = LocalDateTime.now().plusNanos(refreshTokenValidityMs * 1000);

		// RefreshToken 객체 생성
		RefreshToken refreshToken = RefreshToken.builder()
			.id(tokenValue)
			.userHandle(handle)
			.expiryDate(expiryDate)
			.build();

		saveRefreshToken(tokenValue, refreshToken);
		storeUserToken(handle, tokenValue);

		log.info("Created new refresh token for user: {}", handle);
		return tokenValue;
	}

	/**
	 * Refresh Token 객체를 Redis에 저장합니다.
	 *
	 * @param tokenValue   토큰 값
	 * @param refreshToken 저장할 RefreshToken 객체
	 */
	private void saveRefreshToken(String tokenValue, RefreshToken refreshToken) {
		// Redis에 토큰-사용자 매핑 저장 (TTL 설정)
		redisUtil.set(
			TOKEN_PREFIX + tokenValue,
			refreshToken,
			refreshTokenValidityMs,
			TimeUnit.MILLISECONDS
		);
	}

	/**
	 * 사용자의 토큰 목록에 새 토큰을 추가합니다.
	 *
	 * @param handle     사용자 핸들
	 * @param tokenValue 추가할 토큰
	 */
	@SuppressWarnings("unchecked")
	private void storeUserToken(String handle, String tokenValue) {
		String userKey = USER_PREFIX + handle;
		Set<String> userTokens;

		if (redisUtil.hasKey(userKey)) {
			// 기존 토큰 목록에 추가
			userTokens = redisUtil.get(userKey, Set.class);
			if (userTokens == null) {
				userTokens = new HashSet<>();
			}
		} else {
			// 새 토큰 목록 생성
			userTokens = new HashSet<>();
		}

		userTokens.add(tokenValue);
		redisUtil.set(userKey, userTokens, refreshTokenValidityMs, TimeUnit.MILLISECONDS);
	}

	/**
	 * Refresh Token의 유효성을 검증합니다.
	 *
	 * @param token 검증할 리프레시 토큰
	 * @return 검증된 RefreshToken 객체
	 * @throws InvalidTokenException 토큰이 유효하지 않을 경우
	 */
	public RefreshToken validateRefreshToken(String token) {
		// 블랙리스트 확인
		checkIfTokenIsBlacklisted(token);

		// 토큰 존재 확인 및 가져오기
		RefreshToken refreshToken = getRefreshTokenFromRedis(token);

		// 토큰 상태 확인
		validateTokenState(token, refreshToken);

		return refreshToken;
	}

	/**
	 * 토큰이 블랙리스트에 있는지 확인하고, 있으면 관련 조치를 수행합니다.
	 *
	 * @param token 확인할 토큰
	 * @throws InvalidTokenException 토큰이 블랙리스트에 있을 경우
	 */
	private void checkIfTokenIsBlacklisted(String token) {
		if (isTokenBlacklisted(token, TokenType.REFRESH)) {
			log.warn("Token is blacklisted (already used): {}", token);
			// 토큰 도난 시도로 간주하고 사용자의 모든 토큰 무효화
			String handle = jwtUtil.getHandle(token);
			revokeAllUserTokens(handle);
			throw new InvalidTokenException("Refresh token has been used before, possible token theft detected");
		}
	}

	/**
	 * Redis에서 RefreshToken 객체를 가져옵니다.
	 *
	 * @param token 토큰 값
	 * @return RefreshToken 객체
	 * @throws InvalidTokenException 토큰이 존재하지 않거나 파싱할 수 없는 경우
	 */
	private RefreshToken getRefreshTokenFromRedis(String token) {
		String key = TOKEN_PREFIX + token;

		if (!redisUtil.hasKey(key)) {
			log.warn("Token not found in Redis: {}", token);
			throw new InvalidTokenException("Refresh token not found or expired");
		}

		RefreshToken refreshToken = redisUtil.get(key, RefreshToken.class);

		if (refreshToken == null) {
			log.error("Token exists but could not be parsed: {}", token);
			throw new InvalidTokenException("Refresh token data is invalid");
		}

		return refreshToken;
	}

	/**
	 * 토큰의 상태(취소 여부, 만료 여부)를 확인합니다.
	 *
	 * @param token        토큰 값
	 * @param refreshToken RefreshToken 객체
	 * @throws InvalidTokenException 토큰이 취소되었거나 만료된 경우
	 */
	private void validateTokenState(String token, RefreshToken refreshToken) {
		// 취소된 토큰인지 확인
		if (refreshToken.isRevoke()) {
			log.warn("Token has been revoked: {}", token);
			throw new InvalidTokenException("Refresh token was revoked");
		}

		// JWT 자체 검증
		if (jwtUtil.isExpired(token)) {
			log.warn("Token JWT has expired: {}", token);
			redisUtil.delete(TOKEN_PREFIX + token);
			throw new InvalidTokenException("Refresh token has expired");
		}
	}

	/**
	 * Refresh Token을 무효화합니다.
	 *
	 * @param token 무효화할 토큰
	 */
	public void revokeRefreshToken(String token) {
		String key = TOKEN_PREFIX + token;

		// 토큰이 존재하는지 확인
		if (!redisUtil.hasKey(key)) {
			log.debug("Token already expired or not found: {}", token);
			return; // 이미 만료되었거나 존재하지 않음
		}

		// 토큰 정보 가져오기
		RefreshToken refreshToken = redisUtil.get(key, RefreshToken.class);

		if (refreshToken != null) {
			// 토큰 무효화
			refreshToken.revoke();
			redisUtil.set(key, refreshToken, REVOKED_TOKEN_RETENTION_SECONDS,
				TimeUnit.SECONDS); // 5분간 무효화된 상태로 유지 (도난 감지용)

			// 사용자 토큰 목록에서도 제거
			removeTokenFromUserList(refreshToken.getUserHandle(), token);
			log.info("Revoked refresh token for user: {}", refreshToken.getUserHandle());
		}
	}

	/**
	 * 사용자의 토큰 목록에서 토큰을 제거합니다.
	 *
	 * @param handle 사용자 핸들
	 * @param token  제거할 토큰
	 */
	@SuppressWarnings("unchecked")
	private void removeTokenFromUserList(String handle, String token) {
		String userKey = USER_PREFIX + handle;
		if (redisUtil.hasKey(userKey)) {
			Set<String> userTokens = redisUtil.get(userKey, Set.class);
			if (userTokens != null) {
				userTokens.remove(token);

				if (userTokens.isEmpty()) {
					redisUtil.delete(userKey);
				} else {
					redisUtil.set(userKey, userTokens, refreshTokenValidityMs, TimeUnit.MILLISECONDS);
				}
			}
		}
	}

	/**
	 * 사용자의 모든 Refresh Token을 무효화합니다.
	 *
	 * @param handle 사용자 핸들
	 */
	@SuppressWarnings("unchecked")
	public void revokeAllUserTokens(String handle) {
		String userKey = USER_PREFIX + handle;

		// 사용자 토큰 목록 확인
		if (!redisUtil.hasKey(userKey)) {
			log.debug("No tokens found for user: {}", handle);
			return; // 토큰이 없음
		}

		// 사용자의 모든 토큰 가져오기
		Set<String> userTokens = redisUtil.get(userKey, Set.class);

		if (userTokens != null) {
			// 각 토큰 무효화
			for (String token : userTokens) {
				revokeUserToken(token);
			}
		}

		// 사용자 토큰 목록 삭제
		redisUtil.delete(userKey);
		log.info("Revoked all refresh tokens for user: {}", handle);
	}

	/**
	 * 사용자의 단일 토큰을 무효화하는 헬퍼 메서드
	 *
	 * @param token 무효화할 토큰
	 */
	private void revokeUserToken(String token) {
		String tokenKey = TOKEN_PREFIX + token;
		if (redisUtil.hasKey(tokenKey)) {
			RefreshToken refreshToken = redisUtil.get(tokenKey, RefreshToken.class);
			if (refreshToken != null) {
				refreshToken.revoke();
				redisUtil.set(tokenKey, refreshToken, REVOKED_TOKEN_RETENTION_SECONDS,
					TimeUnit.SECONDS); // 5분간 무효화된 상태로 유지
			}
		}
	}

	/**
	 * Refresh Token Rotation - 기존 토큰을 무효화하고 새 토큰 발급
	 *
	 * @param oldToken 이전 토큰
	 * @return 새로 생성된 토큰
	 */
	public String rotateRefreshToken(String oldToken) {
		// 기존 토큰 검증
		RefreshToken oldRefreshToken = validateRefreshToken(oldToken);
		String handle = oldRefreshToken.getUserHandle();

		// 기존 토큰 무효화
		revokeRefreshToken(oldToken);

		// 블랙리스트에 추가 (재사용 방지)
		blacklistToken(oldToken, TokenType.REFRESH);

		// 새 토큰 발급
		return createRefreshToken(handle);
	}
}
