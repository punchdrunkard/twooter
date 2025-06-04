package xyz.twooter.post.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.infrastructure.annotation.CurrentMember;
import xyz.twooter.member.domain.Member;
import xyz.twooter.post.application.PostService;
import xyz.twooter.post.presentation.dto.response.TimelineResponse;

@RestController
@RequestMapping("/api/timeline")
@RequiredArgsConstructor
public class TimelineController {

	// timelineService 로 관련 로직 분리하는게 나을까요?
	private final PostService postService;

	// 현재 로그인한 유저의 타임라인 (유저의 포스트 + 리포스트)를 가져온다.
	@GetMapping
	public ResponseEntity<TimelineResponse> getMyTimeline(
		@RequestParam(required = false) String cursor,
		@RequestParam(required = false, defaultValue = "20") Integer limit,
		@CurrentMember Member member
	) {
		TimelineResponse response = postService.getMyTimeline(cursor, limit, member);
		return ResponseEntity.ok(response);
	}
}
