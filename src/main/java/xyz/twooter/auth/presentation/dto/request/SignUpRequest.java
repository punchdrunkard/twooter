package xyz.twooter.auth.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.twooter.member.domain.Member;
import xyz.twooter.member.domain.ValidHandle;
import xyz.twooter.member.domain.ValidPassword;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SignUpRequest {

	@Email(message = "올바른 이메일 형식이어야 합니다.")
	@NotBlank(message = "이메일은 필수입니다.")
	private String email;
	@ValidHandle
	private String handle;
	@ValidPassword
	private String password;

	public static Member toEntity(SignUpRequest request) {
		return Member.builder()
			.email(request.email)
			.handle(request.handle)
			.password(request.password)
			.build();
	}
}
