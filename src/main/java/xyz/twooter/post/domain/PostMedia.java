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
import xyz.twooter.common.entity.BaseCreateTimeEntity;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_media")
@Entity
@Getter
public class PostMedia extends BaseCreateTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@Column(name = "media_id", nullable = false)
	private Long mediaId;

	@Builder
	public PostMedia(Post post, Long mediaId) {
		this.post = post;
		this.mediaId = mediaId;
	}
}
