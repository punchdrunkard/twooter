package xyz.twooter.member.presentation.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import xyz.twooter.common.infrastructure.pagination.PaginationMetadata;

@Getter
@Builder
@AllArgsConstructor
public class FollowerResponse {

	private List<FollowerProfile> followers;
	private PaginationMetadata metadata;

	public static FollowerResponse of(List<FollowerProfile> followers, Integer limit) {
		return FollowerResponse.builder()
			.followers(followers)
			.metadata(PaginationMetadata.builder()
				.hasNext(followers.size() > limit)
				.build())
			.build();
	}
}
