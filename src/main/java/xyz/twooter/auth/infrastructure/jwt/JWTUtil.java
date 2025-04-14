package xyz.twooter.auth.infrastructure.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import xyz.twooter.auth.domain.TokenType;

@Component
public class JWTUtil {

	private SecretKey secretKey;

	public JWTUtil(@Value("${spring.jwt.secret}") String secret) {

		secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
			Jwts.SIG.HS256.key().build().getAlgorithm());
	}

	public String getHandle(String token) {

		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.get("handle", String.class);
	}

	public Boolean isExpired(String token) {
		try {
			return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload()
				.getExpiration()
				.before(new Date());
		} catch (ExpiredJwtException e) {
			return true; // 이미 만료된 토큰
		}
	}

	public String createJwt(String handle, TokenType type, Long expiredMs) {

		return Jwts.builder()
			.claim("handle", handle)
			.claim("tokenType", type.toString())
			.issuedAt(new Date(System.currentTimeMillis()))
			.expiration(new Date(System.currentTimeMillis() + expiredMs))
			.signWith(secretKey)
			.compact();
	}

	public <T> T getClaim(String token, String claimName, Class<T> requiredType) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload()
			.get(claimName, requiredType);
	}
}
