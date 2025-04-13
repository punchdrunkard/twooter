package xyz.twooter.auth.infrastructure.usersdetails;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.RequiredArgsConstructor;
import xyz.twooter.member.domain.Member;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

	private final Member member;

	public Member getMember() {
		return member;
	}


	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// 기본적으로 모든 사용자에게 "ROLE_USER" 권한 부여
		// 필요에 따라 사용자의 실제 권한을 반환하도록 수정 가능
		return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public String getPassword() {
		return member.getPassword();
	}

	@Override
	public String getUsername() {
		return member.getHandle();
	}

	@Override
	public boolean isAccountNonExpired() {
		// 계정 만료 여부 - 만료되지 않음으로 설정
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		// 계정 잠금 여부 - 잠기지 않음으로 설정
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		// 자격 증명 만료 여부 - 만료되지 않음으로 설정
		return true;
	}

	@Override
	public boolean isEnabled() {
		// 계정 활성화 여부 - 활성화됨으로 설정
		// 필요에 따라 Member 엔티티에 enabled 필드를 추가하여 활성화 여부를 관리할 수 있음
		return true;
	}
}
