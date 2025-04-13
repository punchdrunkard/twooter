package xyz.twooter.member.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.twooter.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsById(Long memberId);

	boolean existsByEmail(String email);

	boolean existsByHandle(String handle);

	Member findByHandle(String handle);
}
