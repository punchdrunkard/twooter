package xyz.twooter.member.domain;

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
@Entity
@Table(
	name = "follow",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_follow_follower_followee",
			columnNames = {"follower_id", "followee_id"})
	}
)
@Getter
public class Follow extends BaseCreateTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "follower_id", nullable = false)
	private Long followerId;

	@Column(name = "followee_id", nullable = false)
	private Long followeeId;

	public boolean isBetween(Long followerId, Long followeeId) {
		return this.followerId.equals(followerId) && this.followeeId.equals(followeeId);
	}

	public boolean isFollowedBy(Long memberId) {
		return this.followerId.equals(memberId);
	}

	@Builder
	public Follow(Long followerId, Long followeeId) {
		this.followerId = followerId;
		this.followeeId = followeeId;
	}

	public static Follow create(Long followerId, Long followeeId) {
		return Follow.builder()
			.followerId(followerId)
			.followeeId(followeeId)
			.build();
	}
}
