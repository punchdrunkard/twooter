package xyz.twooter.auth.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.domain.TokenType;
import xyz.twooter.auth.infrastructure.jwt.JWTUtil;
import xyz.twooter.auth.presentation.dto.request.SignInRequest;
import xyz.twooter.auth.presentation.dto.request.SignUpRequest;
import xyz.twooter.auth.presentation.dto.request.TokenReissueRequest;
import xyz.twooter.auth.presentation.dto.response.LogoutResponse;
import xyz.twooter.auth.presentation.dto.response.SignInResponse;
import xyz.twooter.auth.presentation.dto.response.SignUpInfoResponse;
import xyz.twooter.auth.presentation.dto.response.TokenReissueResponse;
import xyz.twooter.member.application.MemberService;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final MemberService memberService;
	private final AuthenticationManagerBuilder authenticationManagerBuilder;
	private final JWTUtil jwtUtil;
	private final TokenService tokenService;

	@Value("${spring.jwt.access-token-validity}")
	private Long accessTokenValidity;

	@Value("${spring.jwt.refresh-token-validity}")
	private Long refreshTokenValidity;

	@Transactional
	public SignUpInfoResponse signUp(SignUpRequest request) {
		MemberSummaryResponse member = memberService.createMember(request);
		return new SignUpInfoResponse(member);
	}

	public SignInResponse signIn(SignInRequest request) {
		// 인증 토큰 생성
		UsernamePasswordAuthenticationToken authenticationToken =
			new UsernamePasswordAuthenticationToken(request.getHandle(), request.getPassword());

		// 인증 수행
		// 이 과정에서 CustomUserDetailsService.loadUserByUsername()이 호출됨
		Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
		SecurityContextHolder.getContext().setAuthentication(authentication);

		// JWT 토큰 생성
		String accessToken = jwtUtil.createJwt(request.getHandle(), TokenType.ACCESS, accessTokenValidity);
		String refreshToken = tokenService.createRefreshToken(request.getHandle());

		// 사용자 정보 조회
		MemberSummaryResponse memberSummaryResponse = memberService.createMemberSummary(request.getHandle());

		// 리턴
		return new SignInResponse(accessToken, refreshToken, memberSummaryResponse);
	}

	@Transactional
	public TokenReissueResponse reissueToken(TokenReissueRequest request) {
		String oldRefreshToken = request.getRefreshToken();

		// Refresh Token Rotation - 기존 토큰 검증, 무효화, 새 토큰 발급
		String newRefreshToken = tokenService.rotateRefreshToken(oldRefreshToken);

		// 유효한 토큰으로부터 사용자 정보 추출
		String handle = jwtUtil.getHandle(oldRefreshToken);

		// 새 액세스 토큰 발급
		String newAccessToken = jwtUtil.createJwt(handle, TokenType.ACCESS, accessTokenValidity);

		return new TokenReissueResponse(newAccessToken, newRefreshToken);
	}

	/**
	 * 사용자 로그아웃 처리
	 * 액세스 토큰을 블랙리스트에 추가하고 사용자의 모든 리프레시 토큰을 무효화
	 *
	 * @param accessToken 요청 헤더에서 추출한 액세스 토큰
	 * @return 로그아웃 결과
	 */
	@Transactional
	public LogoutResponse logout(String accessToken) {
		try {
			// 액세스 토큰이 유효한지 확인
			if (!jwtUtil.isValid(accessToken)) {
				SecurityContextHolder.clearContext();
				return new LogoutResponse("unknown"); // 유효하지 않은 토큰이어도 로그아웃 성공으로 처리
			}

			// 토큰에서 사용자 정보 추출
			String userHandle = jwtUtil.getHandle(accessToken);

			// 액세스 토큰 블랙리스트에 추가 (만료 시간까지만)
			tokenService.blacklistToken(accessToken, TokenType.ACCESS);

			// 해당 사용자의 모든 리프레시 토큰 무효화
			tokenService.revokeAllUserTokens(userHandle);

			// SecurityContext 초기화
			SecurityContextHolder.clearContext();

			return new LogoutResponse(userHandle);
		} catch (Exception e) {
			// 예외가 발생해도 로그아웃은 성공으로 처리 (보안상 이유)
			return new LogoutResponse("unknown");
		}
	}
}
