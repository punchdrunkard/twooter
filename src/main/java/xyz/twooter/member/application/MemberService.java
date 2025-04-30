package xyz.twooter.member.application;

import java.util.Objects;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.domain.exception.EmailAlreadyExistsException;
import xyz.twooter.auth.domain.exception.IllegalMemberIdException;
import xyz.twooter.auth.presentation.dto.request.SignUpRequest;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.MemberProfile;
import xyz.twooter.member.domain.exception.MemberNotFoundException;
import xyz.twooter.member.domain.repository.MemberProfileRepository;
import xyz.twooter.member.domain.repository.MemberRepository;
import xyz.twooter.member.presentation.dto.MemberSummaryResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;

	@Transactional
	public MemberSummaryResponse createMember(SignUpRequest request) {
		checkDuplicateEmail(request.getEmail());

		Member member = Member.builder()
			.email(request.getEmail())
			.password(bCryptPasswordEncoder.encode(request.getPassword()))
			.handle(request.getHandle())
			.build();

		memberRepository.save(member);
		MemberProfile memberProfile = memberProfileRepository.save(MemberProfile.createDefault(member));

		return MemberSummaryResponse.of(member, memberProfile);
	}

	public MemberSummaryResponse createMemberSummary(String handle) {
		validateMember(handle);

		Member member = memberRepository.findByHandle(handle).orElseThrow(MemberNotFoundException::new);
		MemberProfile memberProfile = memberProfileRepository.findById(member.getId())
			.orElseThrow(MemberNotFoundException::new);

		return MemberSummaryResponse.of(member, memberProfile);
	}

	public MemberSummaryResponse createMemberSummary(Member member) {
		Member foundMember = memberRepository.findById(member.getId())
			.orElseThrow(IllegalMemberIdException::new);

		MemberProfile memberProfile = memberProfileRepository.findById(member.getId())
			.orElseThrow(IllegalMemberIdException::new);

		return MemberSummaryResponse.of(foundMember, memberProfile);
	}

	public void checkDuplicateEmail(String email) {
		if (memberRepository.existsByEmail(email)) {
			throw new EmailAlreadyExistsException();
		}
	}

	public void validateMember(Long memberId) {
		if (Objects.isNull(memberId) || !memberRepository.existsById(memberId)) {
			throw new IllegalMemberIdException();
		}
	}

	public void validateMember(String handle) {
		if (Objects.isNull(handle) || !memberRepository.existsByHandle(handle)) {
			throw new IllegalMemberIdException();
		}
	}
}
