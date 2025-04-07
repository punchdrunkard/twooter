package xyz.twooter.auth.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.twooter.auth.domain.MemberRole;
import xyz.twooter.auth.domain.Role;
import xyz.twooter.member.domain.Member;

public interface MemberRoleRepository extends JpaRepository<MemberRole, Long> {
	boolean existsByMemberAndRole(Member member, Role role);

	List<MemberRole> findAllByMember(Member member);
}
