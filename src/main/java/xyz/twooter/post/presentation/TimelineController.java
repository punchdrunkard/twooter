package xyz.twooter.post.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.infrastructure.annotation.CurrentMember;
import xyz.twooter.member.domain.Member;
import xyz.twooter.post.application.TimelineService;
import xyz.twooter.post.presentation.dto.response.TimelineResponse;

@RestController
@RequestMapping("/api/timeline")
@RequiredArgsConstructor
@Validated
public class TimelineController {

	private final TimelineService timelineService;

	// 현재 로그인한 유저의 타임라인 (유저의 포스트 + 리포스트)를 가져온다.
	@GetMapping("/me")
	public ResponseEntity<TimelineResponse> getMyTimeline(
		@RequestParam(required = false) String cursor,
		@RequestParam(required = false, defaultValue = "20") @Min(value = 1, message = "limit은 1 이상이어야 합니다") Integer limit,
		@CurrentMember Member currentMember
	) {
		TimelineResponse response = timelineService.getTimeline(cursor, limit, currentMember, currentMember.getId());
		return ResponseEntity.ok(response);
	}

	// 특정 유저의 타임라인 (유저의 포스트 + 리포스트)를 가져온다.
	@GetMapping("/user/{userHandle}")
	public ResponseEntity<TimelineResponse> getUserTimeline(
		@RequestParam(required = false) String cursor,
		@RequestParam(required = false, defaultValue = "20") @Min(value = 1, message = "limit은 1 이상이어야 합니다") Integer limit,
		@CurrentMember Member member,
		@PathVariable String userHandle
	) {
		TimelineResponse response = timelineService.getTimelineByHandle(cursor, limit, member, userHandle);
		return ResponseEntity.ok(response);
	}

	// 인증된 사용자의 홈 타임라인을 가져온다.
	@GetMapping("/home")
	public ResponseEntity<TimelineResponse> getHomeTimeline(
		@RequestParam(required = false) String cursor,
		@RequestParam(required = false, defaultValue = "20") @Min(value = 1, message = "limit은 1 이상이어야 합니다") Integer limit,
		@CurrentMember Member currentMember
	) {
		TimelineResponse response = timelineService.getHomeTimeline(cursor, limit, currentMember);
		return ResponseEntity.ok(response);
	}
}
