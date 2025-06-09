package xyz.twooter.common.infrastructure.querydsl;

import java.time.LocalDateTime;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class QueryDslPaginationUtils {

	public static BooleanExpression applyPaginationCondition(
		DateTimePath<LocalDateTime> createdAt,
		NumberPath<Long> id,
		LocalDateTime beforeTimestamp,
		Long beforeId) {

		if (beforeTimestamp == null || beforeId == null) {
			return null;
		}

		return createdAt.lt(beforeTimestamp)
			.or(createdAt.eq(beforeTimestamp).and(id.lt(beforeId)));
	}
}
