package xyz.twooter.member.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import xyz.twooter.common.entity.BaseTimeEntity;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member_profile")
@Entity
public class MemberProfile extends BaseTimeEntity {

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

	// TODO: 아직 금칙어 조항은 만들어지지 않음
	// public static void validateNickName(){
	//
	// }
}
