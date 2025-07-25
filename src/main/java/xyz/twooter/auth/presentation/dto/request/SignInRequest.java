package xyz.twooter.auth.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.twooter.member.domain.ValidHandle;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SignInRequest {

	@ValidHandle
	String handle;

	private String password;
}
