package xyz.twooter.member.presentation.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UnFollowResponse {

	private Long targetMemberId;
	private LocalDateTime unfollowedAt;

	public static UnFollowResponse of(Long targetMemberId) {
		return UnFollowResponse.builder()
			.targetMemberId(targetMemberId)
			.unfollowedAt(LocalDateTime.now())
			.build();
	}
}
