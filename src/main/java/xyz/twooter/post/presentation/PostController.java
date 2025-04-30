package xyz.twooter.post.presentation;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.infrastructure.annotation.CurrentMember;
import xyz.twooter.member.domain.Member;
import xyz.twooter.post.application.PostService;
import xyz.twooter.post.domain.exception.EmptyPostException;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;

	@PostMapping
	public ResponseEntity<PostCreateResponse> createPost(@RequestBody @Valid PostCreateRequest request,
		@CurrentMember Member member) {

		validatePost(request);
		PostCreateResponse response = postService.createPost(request, member);

		URI location = ServletUriComponentsBuilder
			.fromCurrentRequest()
			.build()
			.toUri();

		return ResponseEntity
			.created(location)
			.body(response);
	}

	private static void validatePost(PostCreateRequest request) {
		if ((request.getContent() == null || request.getContent().isBlank()) && (request.getMedia() == null
			|| request.getMedia().length == 0)) {
			throw new EmptyPostException();
		}
	}

}
