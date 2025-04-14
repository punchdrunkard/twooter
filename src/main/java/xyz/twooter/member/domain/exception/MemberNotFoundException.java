package xyz.twooter.member.domain.exception;

import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;

public class MemberNotFoundException extends BusinessException {
	public MemberNotFoundException(String message) {
		super(message, ErrorCode.MEMBER_NOT_FOUND);
	}

	public MemberNotFoundException() {
		super(ErrorCode.MEMBER_NOT_FOUND);
	}
}
