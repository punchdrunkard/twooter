package xyz.twooter.auth.presentation.dto.response;

import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;

public record SignInResponse(
	String accessToken,
	String refreshToken,
	MemberSummaryResponse member
) {
}
