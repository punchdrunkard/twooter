package xyz.twooter.member.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
