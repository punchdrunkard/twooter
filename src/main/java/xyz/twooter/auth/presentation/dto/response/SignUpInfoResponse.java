package xyz.twooter.auth.presentation.dto.response;

import xyz.twooter.member.presentation.dto.MemberSummaryResponse;

public record SignUpInfoResponse(
	MemberSummaryResponse member
) {
}
