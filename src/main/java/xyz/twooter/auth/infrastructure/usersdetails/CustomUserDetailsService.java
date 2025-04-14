package xyz.twooter.auth.infrastructure.usersdetails;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.exception.MemberNotFoundException;
import xyz.twooter.member.domain.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final MemberRepository memberRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String handle) throws UsernameNotFoundException {
		Member member = memberRepository.findByHandle(handle).orElseThrow(MemberNotFoundException::new);
		return new CustomUserDetails(member);
	}
}
