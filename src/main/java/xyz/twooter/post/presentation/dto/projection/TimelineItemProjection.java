package xyz.twooter.post.presentation.dto.projection;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class TimelineItemProjection {

	private final String type;
	private final LocalDateTime feedCreatedAt;
	private final Long originalPostId;
	private final String originalPostContent;
	private final String originalPostAuthorHandle;
	private final String originalPostAuthorNickname;
	private final String originalPostAuthorAvatarPath;
	private final Long likeCount;
	private final Long repostCount;
	private final Long viewCount;
	private final Boolean isLiked;
	private final Boolean isReposted;
	private final Boolean isDeleted;
	private final LocalDateTime originalPostCreatedAt;
	private final String repostAuthorHandle;
	private final String repostAuthorNickname;
	private final String repostAuthorAvatarPath;

	// QueryDSL Projection용 생성자 (정확히 17개 파라미터)
	public TimelineItemProjection(
		String type,                         // 1 "post" or "repost"
		LocalDateTime feedCreatedAt,         // 2
		Long originalPostId,                 // 3
		String originalPostContent,          // 4
		String originalPostAuthorHandle,     // 5
		String originalPostAuthorNickname,   // 6
		String originalPostAuthorAvatarPath, // 7
		Long likeCount,                      // 8
		Long repostCount,                    // 9
		Long viewCount,                      // 10
		Boolean isLiked,                     // 11
		Boolean isReposted,                  // 12
		Boolean isDeleted,                   // 13
		LocalDateTime originalPostCreatedAt, // 14
		String repostAuthorHandle,           // 15
		String repostAuthorNickname,         // 16
		String repostAuthorAvatarPath        // 17
	) {
		this.type = type;
		this.feedCreatedAt = feedCreatedAt;
		this.originalPostId = originalPostId;
		this.originalPostContent = originalPostContent;
		this.originalPostAuthorHandle = originalPostAuthorHandle;
		this.originalPostAuthorNickname = originalPostAuthorNickname;
		this.originalPostAuthorAvatarPath = originalPostAuthorAvatarPath;
		this.likeCount = likeCount;
		this.repostCount = repostCount;
		this.viewCount = viewCount;
		this.isLiked = isLiked;
		this.isReposted = isReposted;
		this.isDeleted = isDeleted;
		this.originalPostCreatedAt = originalPostCreatedAt;
		this.repostAuthorHandle = repostAuthorHandle;
		this.repostAuthorNickname = repostAuthorNickname;
		this.repostAuthorAvatarPath = repostAuthorAvatarPath;
	}

	// 편의 메서드들
	public Long getPostId() {
		return originalPostId; // 타임라인에서 표시할 포스트 ID
	}

	public boolean isRepost() {
		return "repost".equals(type);
	}

	public Long getRepostOfId() {
		return isRepost() ? originalPostId : null;
	}
}
