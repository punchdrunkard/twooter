package xyz.twooter.member.domain;

import java.time.LocalDateTime;

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

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
@Entity
@Getter
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Email
	private String email;

	private String password;

	private String handle;

	private boolean isDeleted;

	private LocalDateTime deletedAt;

	@Builder
	public Member(String email, String password, String handle) {
		validatePassword(password);
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

		// TODO 정규식이 반복되므로 커스텀 애노테이션으로  검증하기
		// 최소 8자 이상, 영문 대/소문자, 숫자, 특수문자 각 1개 이상 포함
		String pattern = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$";

		if (!password.matches(pattern)) {
			throw new InvalidValueException("비밀번호는 최소 8자 이상이며, 영문 대/소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.");
		}
	}

	public static void validateHandle(String handle) {
		if (handle == null || handle.isBlank()) {
			throw new InvalidValueException("핸들은 null이거나 공백일 수 없습니다.");
		}

		// 영문/숫자/밑줄, 4~15자
		String pattern = "^[a-zA-Z0-9_]{4,15}$";

		if (!handle.matches(pattern)) {
			throw new InvalidValueException("핸들은 영문, 숫자, 밑줄(_)만 사용 가능하며 4~15자 사이여야 합니다.");
		}
	}

}
