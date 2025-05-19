package xyz.twooter.common.error;

import com.fasterxml.jackson.annotation.JsonFormat;

// ref: https://github.com/cheese10yun/spring-guide
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ErrorCode {

	/**
	 * Common
	 */
	INVALID_INPUT_VALUE(400, "C001", "입력값이 올바르지 않습니다"),
	METHOD_NOT_ALLOWED(405, "C002", "지원하지 않는 HTTP 메소드입니다"),
	ENTITY_NOT_FOUND(404, "C003", "요청한 리소스를 찾을 수 없습니다"),
	INTERNAL_SERVER_ERROR(500, "C004", "서버 내부 오류가 발생했습니다"),
	INVALID_TYPE_VALUE(400, "C005", "타입이 올바르지 않습니다"),
	HANDLE_ACCESS_DENIED(403, "C006", "접근이 거부되었습니다"),
	MESSAGE_NOT_READABLE(400, "C007", "요청 메시지를 처리할 수 없습니다"),
	MISSING_PARAMETER(400, "C008", "필수 파라미터가 누락되었습니다"),
	/**
	 * Domain
	 */

	// Auth
	ILLEGAL_MEMBER_ID(401, "A001", "유효하지 않은 회원 ID입니다"),
	EMAIL_ALREADY_EXIST(400, "A002", "이미 사용 중인 이메일입니다"),
	MISSING_AUTHORIZATION_HEADER(401, "A003", "인증 헤더가 누락되었습니다"),
	INVALID_TOKEN_FORMAT(401, "A004", "유효하지 않은 토큰 형식입니다"),

	// Member
	MEMBER_NOT_FOUND(404, "M001", "해당 유저를 찾을 수 없습니다"),

	// POST

	// Media
	INVALID_MEDIA_KEY(400, "M001", "해당 키의 미디어가 존재하지 않습니다."),

	// Security (S로 시작하는 코드 사용)
	AUTHENTICATION_FAILED(401, "S001", "인증에 실패했습니다"),
	INVALID_CREDENTIALS(401, "S002", "아이디 또는 비밀번호가 일치하지 않습니다"),
	USER_NOT_FOUND(401, "S003", "존재하지 않는 사용자입니다"),
	ACCOUNT_DISABLED(401, "S004", "비활성화된 계정입니다"),
	ACCOUNT_LOCKED(401, "S005", "계정이 잠겼습니다"),
	ACCESS_DENIED(403, "S006", "접근 권한이 없습니다"),

	// JWT (J로 시작하는 코드 사용)
	TOKEN_EXPIRED(401, "J001", "인증 토큰이 만료되었습니다"),
	INVALID_TOKEN(401, "J002", "유효하지 않은 인증 토큰입니다"),

	// CSRF 및 기타 보안 (X로 시작하는 코드 사용)
	INVALID_CSRF_TOKEN(403, "X001", "유효하지 않은 CSRF 토큰입니다"),
	INVALID_SESSION(401, "X002", "세션이 유효하지 않습니다");

	private final String code;
	private final String message;
	private int status;

	ErrorCode(final int status, final String code, final String message) {
		this.status = status;
		this.message = message;
		this.code = code;
	}

	public String getMessage() {
		return this.message;
	}

	public String getCode() {
		return code;
	}

	public int getStatus() {
		return status;
	}
}
