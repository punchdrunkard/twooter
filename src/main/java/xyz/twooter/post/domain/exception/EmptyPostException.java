package xyz.twooter.post.domain.exception;

import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;

public class EmptyPostException extends BusinessException {
	public EmptyPostException(String message) {
		super(message, ErrorCode.EMPTY_POST);
	}

	public EmptyPostException() {
		super(ErrorCode.EMPTY_POST);
	}
}
