package xyz.twooter.auth.domain.exception;

import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;

public class InvalidTokenException extends BusinessException {
	public InvalidTokenException(String message) {
		super(message, ErrorCode.INVALID_TOKEN);
	}

	public InvalidTokenException() {
		super(ErrorCode.INVALID_TOKEN);
	}
}
