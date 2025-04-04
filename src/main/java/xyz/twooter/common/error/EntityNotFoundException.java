package xyz.twooter.common.error;

// ref: https://github.com/cheese10yun/spring-guide/blob/master/src/main/java/com/spring/guide/global/error/exception/EntityNotFoundException.java
public class EntityNotFoundException extends BusinessException {

	public EntityNotFoundException(String message) {
		super(message, ErrorCode.ENTITY_NOT_FOUND);
	}
}
