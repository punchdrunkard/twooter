package xyz.twooter.member.application;

import java.util.Objects;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.domain.exception.EmailAlreadyExistsException;
import xyz.twooter.auth.domain.exception.IllegalMemberIdException;
import xyz.twooter.auth.presentation.dto.request.SignUpRequest;
import xyz.twooter.member.domain.Follow;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.exception.AlreadyFollowingException;
import xyz.twooter.member.domain.exception.MemberNotFoundException;
import xyz.twooter.member.domain.repository.FollowRepository;
import xyz.twooter.member.domain.repository.MemberRepository;
import xyz.twooter.member.presentation.dto.response.FollowResponse;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;
import xyz.twooter.member.presentation.dto.response.UnFollowResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final FollowRepository followRepository;

	private final BCryptPasswordEncoder bCryptPasswordEncoder;

	@Transactional
	public MemberSummaryResponse createMember(SignUpRequest request) {
		checkDuplicateEmail(request.getEmail());
		Member member = Member.createDefaultMember(request.getEmail(),
			bCryptPasswordEncoder.encode(request.getPassword()), request.getHandle());
		memberRepository.save(member);
		return MemberSummaryResponse.of(member);
	}

	@Transactional
	public FollowResponse followMember(Member member, Long targetMemberId) {
		validateMember(targetMemberId);
		validateFollowing(member, targetMemberId);

		Long followerId = member.getId();
		Long followeeId = targetMemberId;

		Follow follow = Follow.builder()
			.followerId(followerId)
			.followeeId(followeeId)
			.build();
		return FollowResponse.from(followRepository.save(follow));
	}

	public UnFollowResponse unfollowMember(Member member, Long targetMemberId) {
		validateMember(targetMemberId);
		followRepository.deleteByFollowerIdAndFolloweeId(member.getId(), targetMemberId);
		return UnFollowResponse.of(targetMemberId);
	}

	private void validateFollowing(Member member, Long targetMemberId) {
		if (followRepository.existsByFollowerIdAndFolloweeId(member.getId(), targetMemberId)) {
			throw new AlreadyFollowingException();
		}
	}

	public MemberSummaryResponse createMemberSummary(String handle) {
		validateMember(handle);
		Member member = memberRepository.findByHandle(handle).orElseThrow(MemberNotFoundException::new);
		return MemberSummaryResponse.of(member);
	}

	public MemberSummaryResponse createMemberSummary(Member member) {
		Member foundMember = memberRepository.findById(member.getId())
			.orElseThrow(IllegalMemberIdException::new);

		return MemberSummaryResponse.of(foundMember);
	}

	public void checkDuplicateEmail(String email) {
		if (memberRepository.existsByEmail(email)) {
			throw new EmailAlreadyExistsException();
		}
	}

	public void validateMember(String handle) {
		if (Objects.isNull(handle) || !memberRepository.existsByHandle(handle)) {
			throw new IllegalMemberIdException();
		}
	}

	public void validateMember(Long memberId) {
		if (Objects.isNull(memberId) || !memberRepository.existsById(memberId)) {
			throw new IllegalMemberIdException();
		}
	}

}
