package xyz.twooter.member.domain.exception;

import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;

public class SelfFollowException extends BusinessException {
	public SelfFollowException(String message) {
		super(message, ErrorCode.SELF_FOLLOW_NOT_ALLOWED);
	}

	public SelfFollowException() {
		super(ErrorCode.SELF_FOLLOW_NOT_ALLOWED);
	}
}
