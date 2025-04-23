package xyz.twooter.auth.presentation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenReissueRequest {

	@NotNull
	String refreshToken;
}
