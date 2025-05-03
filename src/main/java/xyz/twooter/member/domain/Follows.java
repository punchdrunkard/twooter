package xyz.twooter.member.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.twooter.common.entity.BaseCreateTimeEntity;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
	name = "follows",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_follows_following_followed",
			columnNames = {"following_member_id", "followed_member_id"})
	}
)
@Getter
public class Follows extends BaseCreateTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "following_member_id", nullable = false)
	private Member followingMember;

	@ManyToOne
	@JoinColumn(name = "followed_member_id", nullable = false)
	private Member followedMember;

	@Builder
	public Follows(Member followingMember, Member followedMember) {
		this.followingMember = followingMember;
		this.followedMember = followedMember;
	}
}
