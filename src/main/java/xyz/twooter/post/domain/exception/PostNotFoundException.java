package xyz.twooter.post.domain.exception;

import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;

public class PostNotFoundException extends BusinessException {
	public PostNotFoundException(String message) {
		super(message, ErrorCode.POST_NOT_FOUND);
	}

	public PostNotFoundException() {
		super(ErrorCode.POST_NOT_FOUND);
	}
}
