package xyz.twooter.post.application;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.media.application.MediaService;
import xyz.twooter.media.presentation.dto.response.MediaSimpleResponse;
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
	private final PostMediaRepository postMediaRepository;

	private final MemberService memberService;
	private final MediaService mediaService;

	@Transactional
	public PostCreateResponse createPost(PostCreateRequest request, Member member) {
		Post post = createAndSavePost(request, member);
		MemberSummaryResponse authorSummary = memberService.createMemberSummary(member);

		String[] mediaArray = request.getMedia();
		List<String> mediaUrls = (mediaArray != null) ?
			Arrays.asList(mediaArray) : List.of();

		List<Long> mediaIds = mediaService.saveMedia(mediaUrls);
		savePostMediaMappings(post, mediaIds);

		List<MediaSimpleResponse> mediaResponses = mediaService.getMediaListFromId(mediaIds);
		return PostCreateResponse.of(post, authorSummary, mediaResponses);
	}

	private Post createAndSavePost(PostCreateRequest request, Member member) {
		Post post = Post.builder()
			.author(member)
			.content(request.getContent())
			.build();
		return postRepository.save(post);
	}

	private void savePostMediaMappings(Post post, List<Long> mediaIds) {
		if (mediaIds.isEmpty())
			return;

		List<PostMedia> mappings = mediaIds.stream()
			.map(mediaId -> new PostMedia(post, mediaId))
			.toList();
		postMediaRepository.saveAll(mappings);
	}
}
