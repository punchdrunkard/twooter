package xyz.twooter.member.domain;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import xyz.twooter.member.domain.Member;

@NotBlank(message = "핸들은 필수입니다.")
@Pattern(regexp = Member.HANDLE_PATTERN, message = Member.HANDLE_MESSAGE)
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidHandle {
	String message() default Member.HANDLE_MESSAGE;

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
