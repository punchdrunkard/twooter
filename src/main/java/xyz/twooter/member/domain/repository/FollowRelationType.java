package xyz.twooter.member.domain.repository;

import com.querydsl.core.types.dsl.BooleanExpression;

import xyz.twooter.member.domain.QFollow;
import xyz.twooter.member.domain.QMember;

public enum FollowRelationType {
	FOLLOWERS {
		@Override
		public BooleanExpression getJoinCondition(QFollow follow, QMember member) {
			return follow.followerId.eq(member.id);
		}

		@Override
		public BooleanExpression getWhereCondition(QFollow follow, Long memberId) {
			return follow.followeeId.eq(memberId);
		}

		@Override
		public QMember getTargetMember(QMember follower, QMember following) {
			return follower;
		}
	},
	FOLLOWEES {
		@Override
		public BooleanExpression getJoinCondition(QFollow follow, QMember member) {
			return follow.followeeId.eq(member.id);
		}

		@Override
		public BooleanExpression getWhereCondition(QFollow follow, Long memberId) {
			return follow.followerId.eq(memberId);
		}

		@Override
		public QMember getTargetMember(QMember follower, QMember following) {
			return following;
		}
	};

	public abstract BooleanExpression getJoinCondition(QFollow follow, QMember member);

	public abstract BooleanExpression getWhereCondition(QFollow follow, Long memberId);

	public abstract QMember getTargetMember(QMember follower, QMember following);
}
