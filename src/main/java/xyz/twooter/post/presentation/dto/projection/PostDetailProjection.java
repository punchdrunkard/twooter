package xyz.twooter.post.presentation.dto.projection;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PostDetailProjection {

	private final Long id;
	private final String content;
	private final String authorHandle;
	private final String authorNickname;
	private final String authorAvatar;
	private final LocalDateTime createdAt;
	private final Long viewCount;
	private final Long likeCount;
	private final Long repostCount;
	private final Boolean isLiked;
	private final Boolean isReposted;
	private final Boolean isDeleted;
}
