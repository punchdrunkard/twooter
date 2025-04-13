package xyz.twooter.auth.infrastructure.usersdetails;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.repository.MemberRepository;
import xyz.twooter.support.MockTestSupport;

class CustomUserDetailsServiceTest extends MockTestSupport {
	@Mock
	private MemberRepository memberRepository;

	@InjectMocks
	private CustomUserDetailsService userDetailsService;

	@Test
	@DisplayName("존재하는 사용자 로드")
	void loadExistingUser() {
		// given
		String handle = "testUser";
		Member member = Member.builder()
			.email("email@email.com")
			.handle(handle)
			.password("StrongP@ssw0rd!")
			.build();

		when(memberRepository.existsByHandle(handle)).thenReturn(true);
		when(memberRepository.findByHandle(handle)).thenReturn(member);

		// when
		UserDetails userDetails = userDetailsService.loadUserByUsername(handle);

		// then
		assertThat(userDetails).isNotNull();
		assertThat(userDetails.getUsername()).isEqualTo(handle);
		assertThat(userDetails.getPassword()).isEqualTo("StrongP@ssw0rd!");
		assertThat(userDetails.getAuthorities()).hasSize(1);
		assertThat(userDetails.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
	}

	@Test
	@DisplayName("존재하지 않는 사용자 로드 시 예외 발생")
	void loadNonExistingUser() {
		// given
		String handle = "nonExistingUser";
		when(memberRepository.existsByHandle(handle)).thenReturn(false);

		// when & then
		assertThrows(UsernameNotFoundException.class, () -> {
			userDetailsService.loadUserByUsername(handle);
		});
	}
}
