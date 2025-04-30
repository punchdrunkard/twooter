package xyz.twooter.post.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.media.domain.Media;
import xyz.twooter.media.domain.repository.MediaRepository;
import xyz.twooter.member.application.MemberService;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.presentation.dto.MemberSummaryResponse;
import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.PostMedia;
import xyz.twooter.post.domain.repository.PostMediaRepository;
import xyz.twooter.post.domain.repository.PostRepository;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;
import xyz.twooter.post.presentation.dto.response.PostCreateResponse;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

	private final PostRepository postRepository;

	// 다른 도메인 레파지토리에 접근하는게좀 ...
	// 서비스에 의존하는게 낫지 않을까?
	private final MediaRepository mediaRepository;
	private final PostMediaRepository postMediaRepository;

	// 파사드를 쓸 수 있을까 ?
	private final MemberService memberService;

	@Transactional
	public PostCreateResponse createPost(PostCreateRequest request, Member member) {
		Post post = Post.builder()
			.author(member)
			.content(request.getContent())
			.build();
		postRepository.save(post);

		MemberSummaryResponse memberSummary = memberService.createMemberSummary(member);

		// mediaId가 null이면 빈 리스트 처리
		List<Long> mediaIds = Optional.ofNullable(request.getMedia())
			.map(List::of)
			.orElse(List.of());

		List<Media> mediaList = mediaRepository.findAllByIdIn(mediaIds);

		List<PostMedia> mappings = mediaIds.stream()
			.map(mediaId -> new PostMedia(post, mediaId))
			.toList();
		postMediaRepository.saveAll(mappings);

		return PostCreateResponse.of(post, memberSummary, mediaList);
	}

}
