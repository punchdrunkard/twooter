package xyz.twooter.member.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import lombok.Builder;
import lombok.Getter;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.MemberProfile;

@Getter
public class MemberSummaryResponse {

	@JsonUnwrapped
	private final MemberBasic basicInfo;
	private final String email;

	public MemberSummaryResponse(MemberBasic basicInfo, String email) {
		this.basicInfo = basicInfo;
		this.email = email;
	}

	@Builder
	public MemberSummaryResponse(String handle, String nickname, String avatarPath, String email) {
		this.basicInfo = MemberBasic.builder()
			.nickname(nickname)
			.handle(handle)
			.avatarPath(avatarPath)
			.build();

		this.email = email;
	}

	public static MemberSummaryResponse of(Member member, MemberProfile profile) {
		MemberBasic basicInfo = MemberBasic.of(member, profile);
		return new MemberSummaryResponse(basicInfo, member.getEmail());
	}
}
