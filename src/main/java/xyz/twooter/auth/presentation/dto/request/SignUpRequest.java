package xyz.twooter.auth.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import xyz.twooter.member.domain.Member;

@Builder
@AllArgsConstructor
@Getter
public class SignUpRequest {

	@Email(message = "올바른 이메일 형식이어야 합니다.")
	@NotBlank(message = "이메일은 필수입니다.")
	String email;

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Pattern(
		regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$",
		message = "비밀번호는 최소 8자 이상이며, 영문 대/소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
	)
	private String password;

	@NotBlank(message = "핸들은 필수입니다.")
	@Pattern(
		regexp = "^[a-zA-Z0-9_]{4,15}$",
		message = "핸들은 영문, 숫자, 밑줄(_)만 사용 가능하며 4~15자 사이여야 합니다."
	)
	String handle;

	public static Member toEntity(SignUpRequest request) {
		return Member.builder()
			.email(request.email)
			.handle(request.handle)
			.password(request.password)
			.build();
	}
}
