package xyz.twooter.auth.presentation.dto.response;

import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;

public record SignUpInfoResponse(
	MemberSummaryResponse member
) {
}
