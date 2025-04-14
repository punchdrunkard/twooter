package xyz.twooter.common.error;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.validation.BindingResult;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorResponse {

	private String message;
	private String code;

	private ErrorResponse(final ErrorCode code) {
		this.message = code.getMessage();
		this.code = code.getCode();
	}

	private ErrorResponse(final ErrorCode code, final String customMessage) {
		this.message = customMessage;
		this.code = code.getCode();
	}

	public static ErrorResponse of(final ErrorCode code, final BindingResult bindingResult) {
		// 바인딩 에러의 경우 첫 번째 필드 오류 메시지를 사용
		if (bindingResult.hasFieldErrors()) {
			String errorMessage = bindingResult.getFieldError().getDefaultMessage();
			return new ErrorResponse(code, errorMessage);
		}
		return new ErrorResponse(code);
	}

	public static ErrorResponse of(final ErrorCode code) {
		return new ErrorResponse(code);
	}

	public static ErrorResponse of(MethodArgumentTypeMismatchException e) {
		return new ErrorResponse(ErrorCode.INVALID_TYPE_VALUE,
			String.format("'%s' 필드의 값 '%s'의 타입이 올바르지 않습니다.",
				e.getName(),
				e.getValue() == null ? "" : e.getValue().toString()));
	}
}
