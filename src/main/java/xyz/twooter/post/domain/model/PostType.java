package xyz.twooter.post.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum PostType {
	POST("post"),
	REPOST("repost");

	private final String value;

	public static boolean isRepost(String type) {
		return REPOST.getValue().equals(type);
	}
}
