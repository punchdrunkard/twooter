package xyz.twooter.post.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import xyz.twooter.post.domain.repository.RepostRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RepostService {

	private final RepostRepository repostRepository;

	public long getRepostCount(Long postId) {
		return repostRepository.countByPostId(postId);
	}

	public boolean isRepostedByMember(Long postId, Long memberId) {
		return repostRepository.existsByPostIdAndMemberId(postId, memberId);
	}

}
