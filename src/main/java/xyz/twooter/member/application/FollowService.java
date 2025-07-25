package xyz.twooter.member.application;

import static xyz.twooter.common.infrastructure.pagination.CursorUtil.*;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.twooter.auth.domain.exception.IllegalMemberIdException;
import xyz.twooter.common.infrastructure.pagination.CursorUtil;
import xyz.twooter.common.infrastructure.redis.RedisUtil;
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
import xyz.twooter.post.application.dto.TimelineFanoutMessage;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FollowService {

	private final MemberRepository memberRepository;
	private final FollowRepository followRepository;

	private final RedisUtil redisUtil;
	private final ObjectMapper objectMapper;
	private static final String TIMELINE_QUEUE_KEY = "queue:timeline:fanout";

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

		Follow savedFollow = followRepository.save(follow);

		publishFanoutMessage(TimelineFanoutMessage.ofFollowCreation(followerId, followeeId));

		return FollowResponse.from(savedFollow);
	}

	@Transactional
	public UnFollowResponse unfollowMember(Member member, Long targetMemberId) {
		validateTargetMemberExists(targetMemberId);

		followRepository.deleteByFollowerIdAndFolloweeId(member.getId(), targetMemberId);
		publishFanoutMessage(TimelineFanoutMessage.ofFollowDeletion(member.getId(), targetMemberId));

		return UnFollowResponse.of(targetMemberId);
	}

	public MemberWithRelationResponse getFollowers(String cursor, Integer limit, Member currentMember,
		Long targetMemberId) {
		validateTargetMemberExists(targetMemberId);
		Long viewerId = (currentMember != null) ? currentMember.getId() : null;
		CursorUtil.Cursor decodedCursor = extractCursor(cursor);
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
		CursorUtil.Cursor decodedCursor = extractCursor(cursor);
		int fetchLimit = limit + 1;

		List<MemberProfileWithRelation> followings = followRepository.findFolloweesWithRelation(
				targetMemberId, viewerId,
				decodedCursor != null ? decodedCursor.getTimestamp() : null,
				decodedCursor != null ? decodedCursor.getId() : null,
				fetchLimit)
			.stream()
			.map(MemberProfileWithRelation::fromProjection)
			.toList();

		return MemberWithRelationResponse.of(followings, limit);
	}

	// 공통 메시지 발행 헬퍼 메서드
	private void publishFanoutMessage(TimelineFanoutMessage message) {
		try {
			String messageJson = objectMapper.writeValueAsString(message);
			redisUtil.lPush(TIMELINE_QUEUE_KEY, messageJson);
			log.info("Published fan-out message: {}", messageJson);
		} catch (JsonProcessingException e) {
			log.error("Failed to publish fan-out message: {}", message.toString(), e);
		}
	}

	private void validateTargetMemberExists(Long memberId) {
		if (Objects.isNull(memberId) || !memberRepository.existsById(memberId)) {
			throw new IllegalMemberIdException();
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
