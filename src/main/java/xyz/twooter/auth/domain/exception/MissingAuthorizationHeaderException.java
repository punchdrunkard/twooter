package xyz.twooter.auth.domain.exception;

import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;

public class MissingAuthorizationHeaderException extends BusinessException {
	public MissingAuthorizationHeaderException(String message) {
		super(message, ErrorCode.MISSING_AUTHORIZATION_HEADER);
	}

	public MissingAuthorizationHeaderException() {
		super(ErrorCode.MISSING_AUTHORIZATION_HEADER);
	}
}
