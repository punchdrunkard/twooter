package xyz.twooter.member.presentation.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import xyz.twooter.member.domain.Follow;

@Getter
@Builder
@AllArgsConstructor
public class FollowResponse {

	private Long targetMemberId;
	private LocalDateTime followedAt;

	public static FollowResponse from(Follow follow) {
		return FollowResponse.builder()
			.targetMemberId(follow.getFolloweeId())
			.followedAt(follow.getCreatedAt())
			.build();
	}
}
