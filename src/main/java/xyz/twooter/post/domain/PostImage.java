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
@Table(name = "post_image")
@Entity
@Getter
public class PostImage extends BaseCreateTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@Column(name = "image_id", nullable = false)
	private Long imageId;

	@Column(name = "display_order", nullable = false)
	private Integer displayOrder = 0;

	@Builder
	public PostImage(Post post, Long imageId, Integer displayOrder) {
		this.post = post;
		this.imageId = imageId;
		this.displayOrder = displayOrder;
	}
}
