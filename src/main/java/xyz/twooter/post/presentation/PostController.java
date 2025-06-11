package xyz.twooter.post.presentation;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.infrastructure.annotation.CurrentMember;
import xyz.twooter.member.domain.Member;
import xyz.twooter.post.application.PostLikeService;
import xyz.twooter.post.application.PostService;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;
import xyz.twooter.post.presentation.dto.response.PostDeleteResponse;
import xyz.twooter.post.presentation.dto.response.PostLikeResponse;
import xyz.twooter.post.presentation.dto.response.PostResponse;
import xyz.twooter.post.presentation.dto.response.RepostCreateResponse;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

	private final PostService postService;
	private final PostLikeService postLikeService;

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

	@PatchMapping("/{postId}/like")
	public ResponseEntity<PostLikeResponse> likePost(@PathVariable Long postId, @CurrentMember Member member) {
		PostLikeResponse response = postLikeService.toggleLikeAndCount(postId, member);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{postId}/repost")
	public ResponseEntity<RepostCreateResponse> repost(@PathVariable Long postId, @CurrentMember Member member) {
		RepostCreateResponse response = postService.repostAndIncreaseCount(postId, member);

		URI location = ServletUriComponentsBuilder
			.fromCurrentRequest()
			.build()
			.toUri();

		return ResponseEntity
			.created(location)
			.body(response);
	}

	@DeleteMapping("/{postId}")
	public ResponseEntity<PostDeleteResponse> deletePost(@PathVariable Long postId, @CurrentMember Member member) {
		PostDeleteResponse response = postService.deletePost(postId, member);
		return ResponseEntity.ok(response);
	}
}
