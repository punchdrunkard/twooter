package xyz.twooter.post.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.member.domain.Member;
import xyz.twooter.post.domain.PostLike;
import xyz.twooter.post.domain.exception.PostNotFoundException;
import xyz.twooter.post.domain.repository.PostLikeRepository;
import xyz.twooter.post.domain.repository.PostRepository;
import xyz.twooter.post.presentation.dto.response.PostLikeResponse;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostLikeService {

	private final PostLikeRepository postLikeRepository;
	private final PostRepository postRepository;

	public long getLikeCount(Long postId) {
		return postLikeRepository.countByPostId(postId);
	}

	public boolean isLikedByMember(Long postId, Long memberId) {
		return postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
	}

	@Transactional
	public PostLikeResponse likePost(Long postId, Member member) {
		validateTargetPost(postId);

		boolean isLiked = postLikeRepository.existsByPostIdAndMemberId(postId, member.getId());

		if (isLiked) {
			postLikeRepository.deleteByPostIdAndMemberId(postId, member.getId());
			postLikeRepository.decrementLikeCount(postId);
		} else {
			// 좋아요를 누르지 않은 상태면 좋아요 추가
			postLikeRepository.save(
				PostLike.builder()
					.memberId(member.getId())
					.postId(postId)
					.build());
			postLikeRepository.incrementLikeCount(postId);
		}

		return PostLikeResponse.builder()
			.postId(postId)
			.isLiked(!isLiked)
			.build();
	}

	private void validateTargetPost(Long postId) {
		if (!postRepository.existsById(postId) || postRepository.findIsDeletedById(postId)) {
			throw new PostNotFoundException();
		}
	}
}
