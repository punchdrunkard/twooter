package xyz.twooter.member.presentation.dto;

import lombok.Builder;
import lombok.Getter;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.MemberProfile;

@Getter
public class MemberSummary {

	private final String email;
	private final String handle;
	private final String nickname;
	private final String avatarPath;

	@Builder
	public MemberSummary(String email, String handle, String nickname, String avatarPath) {
		this.email = email;
		this.handle = handle;
		this.nickname = nickname;
		this.avatarPath = avatarPath;
	}

	public static MemberSummary of(Member member, MemberProfile profile) {
		return MemberSummary.builder()
			.email(member.getEmail())
			.handle(member.getHandle())
			.nickname(profile.getNickname())
			.avatarPath(profile.getAvatarPath())
			.build();
	}
}
