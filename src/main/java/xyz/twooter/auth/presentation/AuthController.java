package xyz.twooter.auth.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.application.AuthService;
import xyz.twooter.auth.presentation.dto.request.SignInRequest;
import xyz.twooter.auth.presentation.dto.request.SignUpRequest;
import xyz.twooter.auth.presentation.dto.request.TokenReissueRequest;
import xyz.twooter.auth.presentation.dto.response.LogoutResponse;
import xyz.twooter.auth.presentation.dto.response.SignInResponse;
import xyz.twooter.auth.presentation.dto.response.SignUpInfoResponse;
import xyz.twooter.auth.presentation.dto.response.TokenReissueResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	@PostMapping("/signup")
	public ResponseEntity<SignUpInfoResponse> signUp(@RequestBody @Valid SignUpRequest request) {
		SignUpInfoResponse response = authService.signUp(request);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@PostMapping("/signin")
	public ResponseEntity<SignInResponse> login(@RequestBody @Valid SignInRequest request) {
		SignInResponse response = authService.signIn(request);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@PostMapping("/reissue")
	public ResponseEntity<TokenReissueResponse> reissue(@RequestBody @Valid TokenReissueRequest request) {
		TokenReissueResponse response = authService.reissueToken(request);
		return ResponseEntity.ok().body(response);
	}

	@PostMapping("/logout")
	public ResponseEntity<LogoutResponse> logout(HttpServletRequest request) {
		// 요청 헤더에서 Authorization 토큰 추출
		String authHeader = request.getHeader("Authorization");
		String accessToken = "";

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			accessToken = authHeader.substring(7); // "Bearer " 제거
		}

		// 로그아웃 처리
		LogoutResponse response = authService.logout(accessToken);

		return ResponseEntity.ok(response);
	}
}
