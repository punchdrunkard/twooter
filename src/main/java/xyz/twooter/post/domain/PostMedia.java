package xyz.twooter.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.twooter.common.entity.BaseCreateTimeEntity;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "post_media", uniqueConstraints = {
	@UniqueConstraint(name = "uk_post_media_post_media", columnNames = {"post_id", "media_id"})
})
@Entity
@Getter
public class PostMedia extends BaseCreateTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "media_id", nullable = false)
	private Long mediaId;

	@Builder
	public PostMedia(Long postId, Long mediaId) {
		this.postId = postId;
		this.mediaId = mediaId;
	}
}
