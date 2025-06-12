package xyz.twooter.post.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReplyCreateRequest extends PostCreateRequest {

	@NotNull(message = "답글의 부모 포스트 ID는 필수입니다.")
	private Long parentId;
}
