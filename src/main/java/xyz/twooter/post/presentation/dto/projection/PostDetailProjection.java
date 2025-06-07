package xyz.twooter.post.presentation.dto.projection;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostDetailProjection {

	private final Long postId;
	private final String content;
	private final Long authorId;
	private final String authorHandle;
	private final String authorNickname;
	private final String authorAvatarPath;
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
