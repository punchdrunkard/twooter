package xyz.twooter.member.presentation.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import lombok.Builder;
import lombok.Getter;
import xyz.twooter.member.domain.Member;

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
	public MemberSummaryResponse(Long id, String handle, String nickname, String avatarPath, String email) {
		this.basicInfo = MemberBasic.builder()
			.id(id)
			.nickname(nickname)
			.handle(handle)
			.avatarPath(avatarPath)
			.build();

		this.email = email;
	}

	public static MemberSummaryResponse of(Member member) {
		MemberBasic basicInfo = MemberBasic.of(member);
		return new MemberSummaryResponse(basicInfo, member.getEmail());
	}
}
