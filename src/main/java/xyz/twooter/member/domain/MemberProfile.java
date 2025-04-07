package xyz.twooter.member.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
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

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@NotNull
	private String nickname;
	private String bio;

	@Column(name = "avatar_path")
	private String avatarPath;

	@Builder
	public MemberProfile(Member member, String nickname, String bio, String avatarPath) {
		this.member = member;
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
			.member(member)
			.nickname(member.getHandle())
			.avatarPath(DEFAULT_AVATAR_BASE + member.getHandle())
			.bio("")
			.build();
	}
}
