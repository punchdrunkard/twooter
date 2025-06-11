package xyz.twooter.post.presentation.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import xyz.twooter.common.infrastructure.pagination.PaginationMetadata;

@Builder
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@Getter
public class PostThreadResponse {

	private List<PostResponse> posts;
	private PaginationMetadata metadata;
}
