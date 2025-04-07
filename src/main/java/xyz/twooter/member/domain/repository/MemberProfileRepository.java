package xyz.twooter.member.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import xyz.twooter.member.domain.MemberProfile;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {
}
