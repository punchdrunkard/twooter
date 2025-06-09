package xyz.twooter.member.presentation.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import xyz.twooter.common.infrastructure.pagination.PaginationMetadata;

@Getter
@Builder
@AllArgsConstructor
public class MemberWithRelationResponse {

	private List<MemberProfileWithRelation> members;
	private PaginationMetadata metadata;

	public static MemberWithRelationResponse of(List<MemberProfileWithRelation> members, Integer limit) {
		return MemberWithRelationResponse.builder()
			.members(members)
			.metadata(PaginationMetadata.builder()
				.hasNext(members.size() > limit)
				.build())
			.build();
	}
}
