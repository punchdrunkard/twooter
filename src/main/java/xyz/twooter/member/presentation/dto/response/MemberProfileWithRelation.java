package xyz.twooter.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.twooter.member.domain.repository.projection.MemberProfileProjection;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberProfileWithRelation {

	private Long id;
	private String handle;
	private String nickname;
	private String avatarPath;
	private String bio;

	private boolean isFollowingByMe; // 내가 이 사람을 팔로우하는지
	private boolean followsMe; // 이 사람이 나를 팔로우하는지

	public static MemberProfileWithRelation fromProjection(
		MemberProfileProjection projection) {
		return MemberProfileWithRelation.builder()
			.id(projection.getId())
			.handle(projection.getHandle())
			.nickname(projection.getNickname())
			.avatarPath(projection.getAvatarPath())
			.bio(projection.getBio())
			.isFollowingByMe(projection.isFollowingByMe())
			.followsMe(projection.isFollowsMe())
			.build();
	}
}
