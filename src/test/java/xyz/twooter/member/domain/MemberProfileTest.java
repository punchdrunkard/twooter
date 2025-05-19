package xyz.twooter.member.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberProfileTest {

	@DisplayName("Member로부터 기본 프로필을 생성한다")
	@Test
	void shouldCreateDefaultMemberProfile() {
		// given
		Member member = Member.builder()
			.email("test@example.com")
			.password("StrongP@ssw0rd!")
			.handle("twooter_123")
			.build();

		// when
		MemberProfile profile = MemberProfile.createDefault(member);

		// then
		assertThat(profile.getMemberId()).isEqualTo(member.getId());
		assertThat(profile.getNickname()).isEqualTo("twooter_123");
		assertThat(profile.getBio()).isEmpty();
		assertThat(profile.getAvatarPath())
			.isEqualTo(MemberProfile.DEFAULT_AVATAR_BASE + member.getHandle());
	}
}
