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
import xyz.twooter.auth.presentation.dto.response.SignInResponse;
import xyz.twooter.auth.presentation.dto.response.SignUpInfoResponse;
import xyz.twooter.member.application.MemberService;
import xyz.twooter.member.domain.repository.MemberRepository;
import xyz.twooter.member.presentation.dto.MemberSummary;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final MemberService memberService;
	private final AuthenticationManagerBuilder authenticationManagerBuilder;
	private final JWTUtil jwtUtil;
	private final MemberRepository memberRepository;

	@Value("${spring.jwt.access-token-validity}")
	private Long accessTokenValidity;

	@Value("${spring.jwt.refresh-token-validity}")
	private Long refreshTokenValidity;

	public SignUpInfoResponse signUp(SignUpRequest request) {
		MemberSummary member = memberService.createMember(request);
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
		String refreshToken = jwtUtil.createJwt(request.getHandle(), TokenType.REFRESH, refreshTokenValidity);

		// 사용자 정보 조회
		MemberSummary memberSummary = memberService.createMemberSummary(request.getHandle());

		// 리턴
		return new SignInResponse(accessToken, refreshToken, memberSummary);
	}
}
