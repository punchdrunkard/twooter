package xyz.twooter.auth.presentation.dto.response;

public record TokenReissueResponse(
	String accessToken,
	String refreshToken
) {
}
