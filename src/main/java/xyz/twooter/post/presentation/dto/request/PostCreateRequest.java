package xyz.twooter.post.presentation.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import xyz.twooter.post.domain.validation.ValidPostContent;

@Builder
@Getter
@ValidPostContent
public class PostCreateRequest {

	@Size(max = 500, message = "포스트 내용은 500자 이하로 입력해주세요.")
	private String content;

	@Size(max = 4, message = "미디어는 최대 4개까지만 첨부할 수 있습니다.")
	private String[] media;
}
