package xyz.twooter.post.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.twooter.common.entity.BaseCreateTimeEntity;
import xyz.twooter.member.domain.Member;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
	name = "repost",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_repost_post_member", columnNames = {"post_id", "member_id"})
	},
	indexes = {
		@Index(name = "idx_repost_post_id", columnList = "post_id"),
		@Index(name = "idx_repost_member_id", columnList = "member_id")
	}
)
@Getter
public class Repost extends BaseCreateTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@ManyToOne
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Builder
	public Repost(Post post, Member member) {
		this.post = post;
		this.member = member;
	}
}
