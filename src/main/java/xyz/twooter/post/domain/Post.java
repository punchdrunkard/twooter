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
import xyz.twooter.member.domain.Member;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post")
@Entity
@Getter
public class Post extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "author_id", nullable = false)
	private Member author;

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

	@Builder
	public Post(Member author, String content, Post parentPost, Post quotedPost, Long viewCount) {
		this.author = author;
		this.content = content;
		this.parentPost = parentPost;
		this.quotedPost = quotedPost;
		this.viewCount = viewCount == null ? 0L : viewCount;
	}
}
