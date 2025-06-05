package xyz.twooter.post.presentation.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import xyz.twooter.common.infrastructure.pagination.PaginationMetadata;

@Getter
@Builder
@AllArgsConstructor
public class TimelineResponse {
	private List<TimelineItemResponse> timeline;
	private PaginationMetadata metadata;
}

