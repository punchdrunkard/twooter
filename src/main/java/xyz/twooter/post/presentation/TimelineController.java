package xyz.twooter.post.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
		@RequestParam(required = false, defaultValue = "20") @Min(value = 0, message = "limit은 0 이상이어야 합니다") Integer limit,
		@CurrentMember Member member
	) {
		TimelineResponse response = timelineService.getTimeline(cursor, limit, member, member.getId());
		return ResponseEntity.ok(response);
	}
}
