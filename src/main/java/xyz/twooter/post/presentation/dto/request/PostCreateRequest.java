package xyz.twooter.post.presentation.dto.request;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class PostCreateRequest {
	private String content;
	private Long[] media;
}
