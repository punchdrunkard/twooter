package xyz.twooter.auth.domain;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshToken {

	private String id; // 토큰 값 자체를 ID로 사용
	private String userHandle; // 사용자 식별자
	private boolean revoke;
	private LocalDateTime expiryDate;

	@Builder
	public RefreshToken(String id, String userHandle, boolean revoke, LocalDateTime expiryDate) {
		this.id = id;
		this.userHandle = userHandle;
		this.revoke = revoke;
		this.expiryDate = expiryDate;
	}

	public void revoke() {
		revoke = true;
	}

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(expiryDate);
	}
}
