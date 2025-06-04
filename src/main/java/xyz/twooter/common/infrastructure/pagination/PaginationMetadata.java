package xyz.twooter.common.infrastructure.pagination;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PaginationMetadata {

	private String nextCursor;
	private boolean hasNext;
}
