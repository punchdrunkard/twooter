package xyz.twooter.member.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import xyz.twooter.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsById(Long memberId);

	boolean existsByEmail(String email);

	boolean existsByHandle(String handle);

	Optional<Member> findByHandle(String handle);

	@Query("SELECT m.id FROM Member m WHERE m.handle = :handle")
	Optional<Long> findMemberIdByHandle(String handle);
}
