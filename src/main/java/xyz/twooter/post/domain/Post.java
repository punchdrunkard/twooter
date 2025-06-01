package xyz.twooter.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.twooter.common.entity.BaseTimeEntity;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post")
@Entity
@Getter
public class Post extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "author_id", nullable = false)
	private Long authorId;

	@Column(length = 2000)
	private String content;

	@Column(name = "parent_post_id")
	private Long parentPostId;

	@Column(name = "quoted_post_id")
	private Long quotedPostId;

	@Column(name = "repost_of_id")
	private Long repostOfId;

	@Column(name = "view_count", nullable = false)
	private Long viewCount = 0L;

	// ✅ 반정규화 필드들
	@Column(name = "like_count", nullable = false)
	private Long likeCount = 0L;

	@Column(name = "repost_count", nullable = false)
	private Long repostCount = 0L;

	@Column(name = "is_deleted", nullable = false)
	private Boolean isDeleted = false;

	public String getDisplayContent() {
		if (Boolean.TRUE.equals(this.isDeleted)) {
			return null;
		}
		return this.content;
	}

	public boolean isDeleted() {
		return Boolean.TRUE.equals(this.isDeleted);
	}

	public boolean isRepost() {
		return this.repostOfId != null;
	}

	public boolean isReply() {
		return this.parentPostId != null;
	}

	public boolean isQuote() {
		return this.quotedPostId != null;
	}

	public boolean isOriginalPost() {
		return !isRepost() && !isReply() && !isQuote();
	}

	// ===== 상태 변경 ====
	public void softDelete() {
		this.isDeleted = true;
	}

	public Long incrementViewCount() {
		this.viewCount++;
		return this.viewCount;
	}

	// ==== 생성자 ====
	@Builder
	private Post(Long authorId, String content, Long parentPostId, Long quotedPostId, Long repostOfId,
		Long viewCount, Long likeCount, Long repostCount) {
		this.authorId = authorId;
		this.content = content;
		this.parentPostId = parentPostId;
		this.quotedPostId = quotedPostId;
		this.repostOfId = repostOfId;
		this.viewCount = viewCount != null ? viewCount : 0L;
		this.likeCount = likeCount != null ? likeCount : 0L;
		this.repostCount = repostCount != null ? repostCount : 0L;
	}

	// ==== static factory methods ====
	public static Post createPost(Long authorId, String content) {
		return Post.builder()
			.authorId(authorId)
			.content(content)
			.build();
	}

	public static Post createReply(Long authorId, String content, Long parentPostId) {
		return Post.builder()
			.authorId(authorId)
			.content(content)
			.parentPostId(parentPostId)
			.build();
	}

	public static Post createQuote(Long authorId, String content, Long quotedPostId) {
		return Post.builder()
			.authorId(authorId)
			.content(content)
			.quotedPostId(quotedPostId)
			.build();
	}

	// 내용 없이 원본만 공유
	public static Post createRepost(Long authorId, Long originalPostId) {
		return Post.builder()
			.authorId(authorId)
			.repostOfId(originalPostId)
			.build();
	}
}
