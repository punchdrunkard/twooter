package xyz.twooter.auth.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.twooter.member.domain.ValidHandle;
import xyz.twooter.member.domain.ValidPassword;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SignInRequest {

	@ValidHandle
	String handle;

	@ValidPassword
	private String password;
}
