package xyz.twooter.auth.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import xyz.twooter.member.domain.Member;

@Entity
@Table(name = "member_role",
	uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "role_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberRole {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "role_id", nullable = false)
	private Role role;

	@Builder
	public MemberRole(Member member, Role role) {
		this.member = member;
		this.role = role;
	}

	public static MemberRole assign(Member member, Role role) {
		return MemberRole.builder()
			.member(member)
			.role(role)
			.build();
	}
}
