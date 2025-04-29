package xyz.twooter.auth.domain.exception;

import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;

public class InvalidTokenFormatException extends BusinessException {

	public InvalidTokenFormatException(String message) {
		super(message, ErrorCode.INVALID_TOKEN_FORMAT);
	}

	public InvalidTokenFormatException() {
		super(ErrorCode.INVALID_TOKEN_FORMAT);
	}
}
