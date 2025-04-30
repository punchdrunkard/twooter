package xyz.twooter.media.domain.exception;

import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;

public class InvalidMediaException extends BusinessException {
	public InvalidMediaException(String message) {
		super(message, ErrorCode.INVALID_MEDIA_KEY);
	}

	public InvalidMediaException() {
		super(ErrorCode.INVALID_MEDIA_KEY);
	}
}
