package xyz.twooter.member.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.infrastructure.annotation.CurrentMember;
import xyz.twooter.member.application.MemberService;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.presentation.dto.request.FollowRequest;
import xyz.twooter.member.presentation.dto.response.FollowResponse;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;
import xyz.twooter.member.presentation.dto.response.UnFollowResponse;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Validated
public class MemberController {

	private final MemberService memberService;

	@GetMapping("/me")
	public ResponseEntity<MemberSummaryResponse> getMyInfo(@CurrentMember Member member) {
		return ResponseEntity.status(HttpStatus.OK).body(memberService.createMemberSummary(member.getHandle()));
	}

	@PostMapping("/follow")
	public ResponseEntity<FollowResponse> followMember(@CurrentMember Member member,
		@Valid @RequestBody FollowRequest request) {
		FollowResponse response = memberService.followMember(member, request.getTargetMemberId());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@DeleteMapping("/follow/{targetMemberId}")
	public ResponseEntity<UnFollowResponse> unfollowMember(@CurrentMember Member member,
		@PathVariable Long targetMemberId) {
		UnFollowResponse response = memberService.unfollowMember(member, targetMemberId);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
}
