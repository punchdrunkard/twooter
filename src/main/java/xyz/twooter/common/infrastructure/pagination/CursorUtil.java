package xyz.twooter.common.infrastructure.pagination;

import static xyz.twooter.common.error.ErrorCode.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;

import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import xyz.twooter.common.error.BusinessException;

@Component
@Slf4j
public class CursorUtil {

	private static final String DELIMITER = "#CURSOR#";

	@Getter
	@AllArgsConstructor
	public static class Cursor {
		private LocalDateTime timestamp;
		private Long id;

		public boolean isValid() {
			return timestamp != null && id != null && id > 0;
		}
	}

	public static String encode(Cursor cursor) {
		if (cursor == null || !cursor.isValid()) {
			throw new InvalidCursorException();
		}

		try {
			String data = String.format("%s%s%d",
				cursor.getTimestamp().toString(), DELIMITER, cursor.getId());

			return Base64.getEncoder()
				.encodeToString(data.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			log.error("Failed to encode cursor: {}", cursor, e);
			throw new BusinessException("Failed to encode cursor", INTERNAL_SERVER_ERROR);
		}
	}

	public static Cursor decode(String encodedCursor) {
		if (encodedCursor == null || encodedCursor.trim().isEmpty()) {
			return null;
		}

		try {
			String decoded = new String(
				Base64.getDecoder().decode(encodedCursor),
				StandardCharsets.UTF_8
			);

			return parseCursor(decoded);

		} catch (IllegalArgumentException e) {
			log.warn("Invalid Base64 cursor: {}", encodedCursor);
			throw new InvalidCursorException();
		} catch (Exception e) {
			log.error("Failed to decode cursor: {}", encodedCursor, e);
			throw new InvalidCursorException();

		}
	}

	private static Cursor parseCursor(String decoded) {
		String[] parts = decoded.split(DELIMITER, -1); // -1로 빈 문자열도 포함

		if (parts.length != 2) {
			throw new InvalidCursorException();
		}

		try {
			LocalDateTime timestamp = LocalDateTime.parse(parts[0]);
			Long id = Long.parseLong(parts[1]);

			Cursor cursor = new Cursor(timestamp, id);
			if (!cursor.isValid()) {
				throw new InvalidCursorException();
			}

			return cursor;

		} catch (DateTimeParseException e) {
			throw new InvalidCursorException();
		} catch (NumberFormatException e) {
			throw new InvalidCursorException();
		}
	}

	public String encode(LocalDateTime timestamp, Long id) {
		return encode(new Cursor(timestamp, id));
	}
}
