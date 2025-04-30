package xyz.twooter.support.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import xyz.twooter.auth.infrastructure.usersdetails.CustomUserDetails;
import xyz.twooter.member.domain.Member;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

	@Override
	public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
		SecurityContext context = SecurityContextHolder.createEmptyContext();

		// Member 객체 생성
		Member member = Member.builder()
			.handle(customUser.handle())
			.email(customUser.email())
			.password(customUser.password())
			.build();

		// CustomUserDetails 생성
		CustomUserDetails userDetails = new CustomUserDetails(member);

		// Authentication 객체 생성 및 SecurityContext에 설정
		Authentication auth = new UsernamePasswordAuthenticationToken(
			userDetails, null, userDetails.getAuthorities());
		context.setAuthentication(auth);

		return context;
	}
}
