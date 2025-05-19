package xyz.twooter.member.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import xyz.twooter.common.entity.BaseTimeEntity;
import xyz.twooter.common.error.InvalidValueException;
import xyz.twooter.member.presentation.dto.response.MemberSummaryResponse;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
@Entity
@Getter
public class Member extends BaseTimeEntity {

	public static final String HANDLE_PATTERN = "^[a-zA-Z0-9_]{4,25}$";
	// 최소 8자 이상, 영문 대/소문자, 숫자, 특수문자 각 1개 이상 포함
	public static final String PASSWORD_PATTERN = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*? &])[A-Za-z\\d@$!%*?&]{8,}$";

	public static final String PASSWORD_MESSAGE =
		"비밀번호는 최소 8자 이상이며, 영문 대/소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.";

	public static final String HANDLE_MESSAGE =
		"핸들은 영문, 숫자, 밑줄(_)만 사용 가능하며 4~25자 사이여야 합니다.";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Email
	@Column(unique = true)
	private String email;

	private String password;

	@Column(unique = true)
	private String handle;

	private boolean isDeleted;

	private LocalDateTime deletedAt;

	@Builder
	public Member(String email, String password, String handle) {
		validateHandle(handle);

		this.email = email;
		this.password = password;
		this.handle = handle;
		this.isDeleted = false;
	}

	public static void validatePassword(String password) {
		if (password == null) {
			throw new InvalidValueException("비밀번호는 null일 수 없습니다.");
		}

		if (!password.matches(PASSWORD_PATTERN)) {
			throw new InvalidValueException(PASSWORD_MESSAGE);
		}
	}

	public static void validateHandle(String handle) {
		if (handle == null || handle.isBlank()) {
			throw new InvalidValueException("핸들은 null이거나 공백일 수 없습니다.");
		}

		if (!handle.matches(HANDLE_PATTERN)) {
			throw new InvalidValueException(HANDLE_MESSAGE);
		}
	}

	public MemberSummaryResponse toSummary(MemberProfile profile) {
		return MemberSummaryResponse.of(this, profile);
	}
}
