package xyz.twooter.auth.presentation.dto.response;

import xyz.twooter.member.presentation.dto.MemberSummary;

public record SignInResponse(
	String accessToken,
	String refreshToken,
	MemberSummary member
) {
}
