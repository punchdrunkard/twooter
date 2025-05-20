package xyz.twooter.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.twooter.common.entity.BaseTimeEntity;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_profile")
@Getter
@Entity
public class MemberProfile extends BaseTimeEntity {

	public static final String DEFAULT_AVATAR_BASE = "https://avatar.iran.liara.run/username?username=";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false, unique = true)
	private Long memberId;

	@NotNull
	private String nickname;

	@Column(length = 1000)
	private String bio;

	@Column(name = "avatar_path")
	private String avatarPath;

	@Builder
	public MemberProfile(Long memberId, String nickname, String bio, String avatarPath) {
		this.memberId = memberId;
		this.nickname = nickname;
		this.bio = bio;
		this.avatarPath = avatarPath;
	}

	// TODO: 아직 금칙어 조항은 만들어지지 않음
	// public static void validateNickName(){
	//
	// }

	public static MemberProfile createDefault(Member member) {
		return MemberProfile.builder()
			.memberId(member.getId())
			.nickname(member.getHandle())
			.avatarPath(DEFAULT_AVATAR_BASE + member.getHandle())
			.bio("")
			.build();
	}
}
