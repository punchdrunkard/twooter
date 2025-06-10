package xyz.twooter.post.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PostLikeResponse {
	private Long postId;
	private Boolean isLiked;
}
