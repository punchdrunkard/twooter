package xyz.twooter.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

	private String content;

	@ManyToOne
	@JoinColumn(name = "parent_post_id")
	private Post parentPost;

	@ManyToOne
	@JoinColumn(name = "quoted_post_id")
	private Post quotedPost;

	@Column(name = "view_count", nullable = false)
	private Long viewCount = 0L;

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

	public void softDelete() {
		this.isDeleted = true;
	}

	@Builder
	public Post(Long authorId, String content, Post parentPost, Post quotedPost, Long viewCount) {
		this.authorId = authorId;
		this.content = content;
		this.parentPost = parentPost;
		this.quotedPost = quotedPost;
		this.viewCount = viewCount == null ? 0L : viewCount;
	}
}
