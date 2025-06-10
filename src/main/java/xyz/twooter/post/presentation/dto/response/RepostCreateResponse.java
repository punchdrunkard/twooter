package xyz.twooter.post.presentation.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RepostCreateResponse {

	private Long repostId;
	private Long originalPostId;
	private LocalDateTime repostedAt;
}
