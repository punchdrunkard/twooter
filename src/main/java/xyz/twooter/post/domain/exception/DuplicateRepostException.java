package xyz.twooter.post.domain.exception;

import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;

public class DuplicateRepostException extends BusinessException {
	public DuplicateRepostException(String message) {
		super(message, ErrorCode.DUPLICATE_REPOST);
	}

	public DuplicateRepostException() {
		super(ErrorCode.DUPLICATE_REPOST);
	}
}
