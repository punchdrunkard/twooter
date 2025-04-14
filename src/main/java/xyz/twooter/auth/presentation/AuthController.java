package xyz.twooter.auth.presentation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import xyz.twooter.auth.application.AuthService;
import xyz.twooter.auth.presentation.dto.request.SignInRequest;
import xyz.twooter.auth.presentation.dto.request.SignUpRequest;
import xyz.twooter.auth.presentation.dto.response.SignInResponse;
import xyz.twooter.auth.presentation.dto.response.SignUpInfoResponse;

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
}
