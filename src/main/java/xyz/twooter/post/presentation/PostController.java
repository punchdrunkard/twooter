package xyz.twooter.post.presentation;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.infrastructure.annotation.CurrentMember;
import xyz.twooter.member.domain.Member;
import xyz.twooter.post.application.PostService;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostResponse;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@GetMapping("/{postId}")
	public ResponseEntity<PostResponse> getPost(@PathVariable Long postId, @CurrentMember Member member) {
		postService.incrementViewCount(postId);
		PostResponse response = postService.getPost(postId, member);
		return ResponseEntity.ok(response);
	}

	@PostMapping
	public ResponseEntity<PostCreateResponse> createPost(@RequestBody @Valid PostCreateRequest request,
		@CurrentMember Member member) {
		PostCreateResponse response = postService.createPost(request, member);

		URI location = ServletUriComponentsBuilder
			.fromCurrentRequest()
			.build()
			.toUri();

		return ResponseEntity
			.created(location)
			.body(response);
	}
}
