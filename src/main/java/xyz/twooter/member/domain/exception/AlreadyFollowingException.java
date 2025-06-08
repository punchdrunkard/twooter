package xyz.twooter.member.domain.exception;

import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;

public class AlreadyFollowingException extends BusinessException {

	public AlreadyFollowingException(String message) {
		super(message, ErrorCode.FOLLOW_ALREADY_EXISTS);
	}

	public AlreadyFollowingException() {
		super(ErrorCode.FOLLOW_ALREADY_EXISTS);
	}
}
