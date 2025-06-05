package xyz.twooter.common.infrastructure.pagination;

import static xyz.twooter.common.error.ErrorCode.*;

import xyz.twooter.common.error.BusinessException;

public class InvalidCursorException extends BusinessException {
	public InvalidCursorException(String message) {
		super(message, INVALID_CURSOR);
	}

	public InvalidCursorException() {
		super(INVALID_CURSOR);
	}
}
