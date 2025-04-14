package xyz.twooter.auth.domain.exception;

import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;

public class EmailAlreadyExistsException extends BusinessException {

	public EmailAlreadyExistsException(String message) {
		super(message, ErrorCode.EMAIL_ALREADY_EXIST);
	}

	public EmailAlreadyExistsException() {
		super(ErrorCode.EMAIL_ALREADY_EXIST);
	}
}
