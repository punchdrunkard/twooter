package xyz.twooter.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import xyz.twooter.common.entity.BaseTimeEntity;
import xyz.twooter.member.domain.Member;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "post",
	indexes = {
		@Index(name = "idx_post_author_id", columnList = "author_id"),
		@Index(name = "idx_post_created_at", columnList = "created_at")
	}
)
@Entity
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
}
