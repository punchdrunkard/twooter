package xyz.twooter.member.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import xyz.twooter.common.error.InvalidValueException;

class MemberTest {

	@Nested
	@DisplayName("비밀번호 검증")
	class ValidatePasswordTest {

		@Test
		@DisplayName("모든 조건을 만족하는 비밀번호는 통과한다")
		void shouldPassWhenPasswordMeetsAllConditions() {
			assertThatCode(() -> Member.validatePassword("StrongP@ssw0rd!"))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("비밀번호가 null이면 예외 발생")
		void shouldFailWhenPasswordIsNull() {
			assertThatThrownBy(() -> Member.validatePassword(null))
				.isInstanceOf(InvalidValueException.class)
				.hasMessageContaining("비밀번호는 null일 수 없습니다.");
		}

		@Test
		@DisplayName("비밀번호가 8자 미만이면 예외 발생")
		void shouldFailWhenPasswordTooShort() {
			assertThatThrownBy(() -> Member.validatePassword("Aa1!a"))
				.isInstanceOf(InvalidValueException.class);
		}

		@Test
		@DisplayName("영문 대문자가 없으면 예외 발생")
		void shouldFailWhenMissingUppercase() {
			assertThatThrownBy(() -> Member.validatePassword("strongp@ss1"))
				.isInstanceOf(InvalidValueException.class);
		}

		@Test
		@DisplayName("영문 소문자가 없으면 예외 발생")
		void shouldFailWhenMissingLowercase() {
			assertThatThrownBy(() -> Member.validatePassword("STRONGP@SS1"))
				.isInstanceOf(InvalidValueException.class);
		}

		@Test
		@DisplayName("숫자가 없으면 예외 발생")
		void shouldFailWhenMissingDigit() {
			assertThatThrownBy(() -> Member.validatePassword("StrongP@ss"))
				.isInstanceOf(InvalidValueException.class);
		}

		@Test
		@DisplayName("특수문자가 없으면 예외 발생")
		void shouldFailWhenMissingSpecialChar() {
			assertThatThrownBy(() -> Member.validatePassword("StrongPass1"))
				.isInstanceOf(InvalidValueException.class);
		}
	}

	@Nested
	@DisplayName("핸들 검증")
	class ValidateHandleTest {

		@Test
		@DisplayName("모든 조건을 만족하는 핸들은 통과한다")
		void shouldPassWhenHandleValid() {
			assertThatCode(() -> Member.validateHandle("valid_handle1"))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("핸들이 null이면 예외 발생")
		void shouldFailWhenHandleIsNull() {
			assertThatThrownBy(() -> Member.validateHandle(null))
				.isInstanceOf(InvalidValueException.class)
				.hasMessageContaining("핸들은 null이거나 공백일 수 없습니다.");
		}

		@Test
		@DisplayName("핸들이 공백이면 예외 발생")
		void shouldFailWhenHandleIsBlank() {
			assertThatThrownBy(() -> Member.validateHandle(" "))
				.isInstanceOf(InvalidValueException.class);
		}

		@Test
		@DisplayName("핸들이 4자 미만이면 예외 발생")
		void shouldFailWhenHandleTooShort() {
			assertThatThrownBy(() -> Member.validateHandle("abc"))
				.isInstanceOf(InvalidValueException.class);
		}

		@Test
		@DisplayName("핸들이 25자 초과면 예외 발생")
		void shouldFailWhenHandleTooLong() {
			assertThatThrownBy(() -> Member.validateHandle("a".repeat(26)))
				.isInstanceOf(InvalidValueException.class);
		}

		@Test
		@DisplayName("핸들에 허용되지 않은 문자가 포함되면 예외 발생")
		void shouldFailWhenHandleContainsInvalidChars() {
			assertThatThrownBy(() -> Member.validateHandle("handle!@#"))
				.isInstanceOf(InvalidValueException.class);
		}
	}
}
