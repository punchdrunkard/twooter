package xyz.twooter.post.domain.repository.projection;

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
	private final Long originalPostAuthorId;
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
	private final Long repostAuthorId;
	private final String repostAuthorHandle;
	private final String repostAuthorNickname;
	private final String repostAuthorAvatarPath;

	public TimelineItemProjection(
		// type: "repost" or "post"
		String type,
		// feedCreatedAt: 타임라인 정렬 기준
		LocalDateTime feedCreatedAt,
		// originalPostId: 실제 표시할 포스트 ID
		Long originalPostId,
		String originalPostContent,
		// author: 실제 포스트 작성자 정보
		Long originalPostAuthorId,
		String originalPostAuthorHandle,
		String originalPostAuthorNickname,
		String originalPostAuthorAvatarPath,
		// 통계: 원본 포스트 기준
		Long likeCount,
		Long repostCount,
		Long viewCount,
		// 상호작용 관련 정보 - 현재 사용자 기준
		Boolean isLiked,
		Boolean isReposted,
		Boolean isDeleted,
		LocalDateTime originalPostCreatedAt,
		// 리포스터 정보 (리포스트인 경우에만)
		Long repostAuthorId,
		String repostAuthorHandle,
		String repostAuthorNickname,
		String repostAuthorAvatarPath) {
		this.type = type;
		this.feedCreatedAt = feedCreatedAt;
		this.originalPostId = originalPostId;
		this.originalPostContent = originalPostContent;
		this.originalPostAuthorId = originalPostAuthorId;
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
		this.repostAuthorId = repostAuthorId;
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
