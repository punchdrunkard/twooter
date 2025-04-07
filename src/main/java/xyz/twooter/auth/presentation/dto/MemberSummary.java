package xyz.twooter.auth.presentation.dto;

public record MemberSummary(
	String email,
	String handle,
	String nickname,
	String avatarPath
) {
}
