package xyz.twooter.member.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;
import xyz.twooter.member.domain.Member;

@Getter
public class MemberBasic {

	private final Long id;
	private final String handle;
	private final String nickname;
	private final String avatarPath;

	@Builder
	public MemberBasic(Long id, String handle, String nickname, String avatarPath) {
		this.id = id;
		this.handle = handle;
		this.nickname = nickname;
		this.avatarPath = avatarPath;
	}

	public static MemberBasic of(Member member) {
		return MemberBasic.builder()
			.id(member.getId())
			.handle(member.getHandle())
			.nickname(member.getNickname())
			.avatarPath(member.getAvatarPath())
			.build();
	}
}
