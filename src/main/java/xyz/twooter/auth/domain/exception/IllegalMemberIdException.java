package xyz.twooter.auth.domain.exception;

import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;

public class IllegalMemberIdException extends BusinessException {
	public IllegalMemberIdException(String message) {
		super(message, ErrorCode.ILLEGAL_MEMBER_ID);
	}

	public IllegalMemberIdException() {
		super(ErrorCode.ILLEGAL_MEMBER_ID);
	}
}
