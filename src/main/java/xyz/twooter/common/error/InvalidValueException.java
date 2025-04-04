package xyz.twooter.common.error;

// ref: https://github.com/cheese10yun/spring-guide/blob/master/src/main/java/com/spring/guide/global/error/exception/InvalidValueException.java
public class InvalidValueException extends BusinessException {

	public InvalidValueException(String value) {
		super(value, ErrorCode.INVALID_INPUT_VALUE);
	}

	public InvalidValueException(String value, ErrorCode errorCode) {
		super(value, errorCode);
	}
}
