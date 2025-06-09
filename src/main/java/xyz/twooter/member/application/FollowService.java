package xyz.twooter.member.application;

import static xyz.twooter.common.infrastructure.pagination.CursorUtil.*;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.domain.exception.IllegalMemberIdException;
import xyz.twooter.common.infrastructure.pagination.CursorUtil;
import xyz.twooter.member.domain.Follow;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.exception.AlreadyFollowingException;
import xyz.twooter.member.domain.exception.SelfFollowException;
import xyz.twooter.member.domain.repository.FollowRepository;
import xyz.twooter.member.domain.repository.MemberRepository;
import xyz.twooter.member.presentation.dto.response.FollowResponse;
import xyz.twooter.member.presentation.dto.response.MemberProfileWithRelation;
import xyz.twooter.member.presentation.dto.response.MemberWithRelationResponse;
import xyz.twooter.member.presentation.dto.response.UnFollowResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

	private final MemberRepository memberRepository;
	private final FollowRepository followRepository;

	@Transactional
	public FollowResponse followMember(Member member, Long targetMemberId) {
		validateTargetMemberExists(targetMemberId);
		validateFollowing(member, targetMemberId);

		Long followerId = member.getId();
		Long followeeId = targetMemberId;

		Follow follow = Follow.builder()
			.followerId(followerId)
			.followeeId(followeeId)
			.build();
		return FollowResponse.from(followRepository.save(follow));
	}

	@Transactional
	public UnFollowResponse unfollowMember(Member member, Long targetMemberId) {
		validateTargetMemberExists(targetMemberId);
		followRepository.deleteByFollowerIdAndFolloweeId(member.getId(), targetMemberId);
		return UnFollowResponse.of(targetMemberId);
	}

	public MemberWithRelationResponse getFollowers(String cursor, Integer limit, Member currentMember,
		Long targetMemberId) {

		validateTargetMemberExists(targetMemberId);
		Long viewerId = (currentMember != null) ? currentMember.getId() : null;

		// 커서 디코딩 (null 가능)
		CursorUtil.Cursor decodedCursor = extractCursor(cursor);
		// 다음 페이지 존재 여부 확인을 위해 +1
		int fetchLimit = limit + 1;

		List<MemberProfileWithRelation> followers = followRepository.findFollowersWithRelation(
				targetMemberId, viewerId,
				decodedCursor != null ? decodedCursor.getTimestamp() : null,
				decodedCursor != null ? decodedCursor.getId() : null,
				fetchLimit)
			.stream()
			.map(MemberProfileWithRelation::fromProjection)
			.toList();

		return MemberWithRelationResponse.of(followers, limit);
	}

	public MemberWithRelationResponse getFollowing(String cursor, Integer limit, Member currentMember,
		Long targetMemberId) {

		validateTargetMemberExists(targetMemberId);
		Long viewerId = (currentMember != null) ? currentMember.getId() : null;

		// 커서 디코딩 (null 가능)
		CursorUtil.Cursor decodedCursor = extractCursor(cursor);
		// 다음 페이지 존재 여부 확인을 위해 +1
		int fetchLimit = limit + 1;

		List<MemberProfileWithRelation> followers = followRepository.findFolloweesWithRelation(
				targetMemberId, viewerId,
				decodedCursor != null ? decodedCursor.getTimestamp() : null,
				decodedCursor != null ? decodedCursor.getId() : null,
				fetchLimit)
			.stream()
			.map(MemberProfileWithRelation::fromProjection)
			.toList();

		return MemberWithRelationResponse.of(followers, limit);
	}

	private void validateTargetMemberExists(Long memberId) {
		if (Objects.isNull(memberId) || !memberRepository.existsById(memberId)) {
			throw new IllegalMemberIdException(); // 또는 MemberNotFoundException
		}
	}

	private void validateFollowing(Member member, Long targetMemberId) {
		if (followRepository.existsByFollowerIdAndFolloweeId(member.getId(), targetMemberId)) {
			throw new AlreadyFollowingException();
		}

		if (Objects.equals(member.getId(), targetMemberId)) {
			throw new SelfFollowException();
		}
	}

}
