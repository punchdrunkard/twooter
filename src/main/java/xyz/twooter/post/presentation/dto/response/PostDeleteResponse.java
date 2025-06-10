package xyz.twooter.post.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class PostDeleteResponse {
	private final Long postId;
}
