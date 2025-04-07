package xyz.twooter.auth.presentation.dto.response;

import xyz.twooter.auth.presentation.dto.MemberSummary;

public record SignUpInfoResponse(
	String accessToken,
	String refreshToken,
	MemberSummary member
) {
}
