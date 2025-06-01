package xyz.twooter.post.presentation.dto.projection;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostDetailProjection {

	private final Long id;
	private final String content;
	private final String handle;
	private final String nickname;
	private final String avatarPath;
	private final LocalDateTime createdAt;
	private final Long viewCount;
	private final Long likeCount;
	private final Long repostCount;
	private final Boolean isLiked;
	private final Boolean isReposted;
	private final Boolean isDeleted;
	private final Long parentPostId;
	private final Long quotedPostId;
	private final Long repostOfId;
}
