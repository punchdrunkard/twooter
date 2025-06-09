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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.infrastructure.annotation.CurrentMember;
import xyz.twooter.member.application.FollowService;
import xyz.twooter.member.application.MemberService;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.presentation.dto.request.FollowRequest;
import xyz.twooter.member.presentation.dto.response.FollowResponse;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;
import xyz.twooter.member.presentation.dto.response.MemberWithRelationResponse;
import xyz.twooter.member.presentation.dto.response.UnFollowResponse;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Validated
public class MemberController {

	private final MemberService memberService;
	private final FollowService followService;

	@GetMapping("/me")
	public ResponseEntity<MemberSummaryResponse> getMyInfo(@CurrentMember Member member) {
		return ResponseEntity.status(HttpStatus.OK).body(memberService.createMemberSummary(member.getHandle()));
	}

	@PostMapping("/follow")
	public ResponseEntity<FollowResponse> followMember(@CurrentMember Member member,
		@Valid @RequestBody FollowRequest request) {
		FollowResponse response = followService.followMember(member, request.getTargetMemberId());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@DeleteMapping("/follow/{targetMemberId}")
	public ResponseEntity<UnFollowResponse> unfollowMember(@CurrentMember Member member,
		@PathVariable Long targetMemberId) {
		UnFollowResponse response = followService.unfollowMember(member, targetMemberId);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	// 해당 멤버의 팔로워 목록을 조회하는 API
	@GetMapping("/{memberId}/followers")
	public ResponseEntity<MemberWithRelationResponse> getFollowers(
		@RequestParam(required = false) String cursor,
		@RequestParam(required = false, defaultValue = "20") @Min(value = 1, message = "limit은 1 이상이어야 합니다") Integer limit,
		@CurrentMember Member currentMember,
		@PathVariable Long memberId
	) {
		MemberWithRelationResponse response = followService.getFollowers(cursor, limit, currentMember, memberId);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@GetMapping("/{memberId}/followings")
	public ResponseEntity<MemberWithRelationResponse> getFollowing(
		@RequestParam(required = false) String cursor,
		@RequestParam(required = false, defaultValue = "20") @Min(value = 1, message = "limit은 1 이상이어야 합니다") Integer limit,
		@CurrentMember Member currentMember,
		@PathVariable Long memberId
	) {
		MemberWithRelationResponse response = followService.getFollowing(cursor, limit, currentMember, memberId);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

}
