package xyz.twooter.post.application;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.twooter.common.infrastructure.redis.RedisUtil;
import xyz.twooter.member.domain.repository.FollowRepository;
import xyz.twooter.post.application.dto.EventType;
import xyz.twooter.post.application.dto.TimelineFanoutMessage;
import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.repository.PostRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class TimelineQueueListener implements SmartLifecycle {
	
	private final RedisUtil redisUtil;
	private final ObjectMapper objectMapper;
	private final FollowRepository followRepository;
	private final PostRepository postRepository;

	private final ExecutorService executorService = Executors.newFixedThreadPool(10);
	private Thread listenerThread;
	private volatile boolean isRunning = false;

	public static final int TIMELINE_CACHE_LIMIT = 1000;
	private static final String TIMELINE_QUEUE_KEY = "queue:timeline:fanout";
	private static final String TIMELINE_ZSET_PREFIX = "timeline:user:";

	private Map<EventType, Consumer<TimelineFanoutMessage>> handlers;

	@PostConstruct
	public void initializeHandlers() {
		this.handlers = Map.of(
			EventType.POST_CREATED, this::handlePostCreated,
			EventType.POST_DELETED, this::handlePostDeleted,
			EventType.FOLLOW_CREATED, this::handleFollowCreated,
			EventType.UNFOLLOW_CREATED, this::handleFollowDeleted
		);
	}

	@Override
	public void start() {
		log.info("Starting TimelineQueueListener...");
		this.listenerThread = new Thread(this::processQueue);
		this.listenerThread.start();
		this.isRunning = true;
	}

	@Override
	public void stop() {
		log.info("Stopping TimelineQueueListener...");
		this.isRunning = false;
		if (this.listenerThread != null) {
			this.listenerThread.interrupt();
		}
		this.executorService.shutdown();
		try {
			if (!this.executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				this.executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			this.executorService.shutdownNow();
		}
		log.info("TimelineQueueListener stopped.");
	}

	@Override
	public boolean isRunning() {
		return this.isRunning;
	}

	private void processQueue() {
		log.info("Timeline fan-out queue listener thread started.");
		while (isRunning && !Thread.currentThread().isInterrupted()) {
			try {
				String messageJson = redisUtil.brPop(TIMELINE_QUEUE_KEY, 3, TimeUnit.SECONDS);
				if (messageJson != null) {
					executorService.submit(() -> fanout(messageJson));
				}
			} catch (Exception e) {
				if (!isRunning) {
					break;
				}
				log.error("Error while processing timeline queue", e);
			}
		}
		log.info("Timeline fan-out queue listener thread finished.");
	}

	private void fanout(String messageJson) {
		try {
			TimelineFanoutMessage message = objectMapper.readValue(messageJson, TimelineFanoutMessage.class);
			Consumer<TimelineFanoutMessage> handler = handlers.get(message.getType());
			if (handler != null) {
				handler.accept(message);
			} else {
				log.warn("No handler found for event type: {}", message.getType());
			}
		} catch (Exception e) {
			log.error("Failed to execute fan-out for message: {}", messageJson, e);
		}
	}

	private void handlePostCreated(TimelineFanoutMessage message) {
		Long postId = message.getPostId();
		Long authorId = message.getAuthorId();
		double score = message.getCreatedAt().toEpochSecond(ZoneOffset.UTC);

		List<Long> followerIds = followRepository.findAllFollowerIdsByFolloweeId(authorId);
		List<Long> targetUserIds = new ArrayList<>(followerIds);
		targetUserIds.add(authorId);

		for (Long userId : targetUserIds) {
			String timelineKey = TIMELINE_ZSET_PREFIX + userId;
			redisUtil.zAdd(timelineKey, String.valueOf(postId), score);
			redisUtil.zRemRangeByRank(timelineKey, TIMELINE_CACHE_LIMIT);
		}
		log.info("Fan-out complete for POST_CREATED, postId: {}", postId);
	}

	private void handlePostDeleted(TimelineFanoutMessage message) {
		Long postId = message.getPostId();
		Long authorId = message.getAuthorId();

		List<Long> followerIds = followRepository.findAllFollowerIdsByFolloweeId(authorId);
		List<Long> targetUserIds = new ArrayList<>(followerIds);
		targetUserIds.add(authorId);

		for (Long userId : targetUserIds) {
			redisUtil.zRem(TIMELINE_ZSET_PREFIX + userId, String.valueOf(postId));
		}
		log.info("Fan-out complete for POST_DELETED, postId: {}", postId);
	}

	private void handleFollowCreated(TimelineFanoutMessage message) {
		Long followerId = message.getFollowerId();
		Long followeeId = message.getFolloweeId();

		List<Post> recentPosts = postRepository.findTop50ByAuthorIdAndIsDeletedFalseAndRepostOfIdIsNullOrderByIdDesc(
			followeeId);

		String followerTimelineKey = TIMELINE_ZSET_PREFIX + followerId;
		for (Post post : recentPosts) {
			double score = post.getCreatedAt().toEpochSecond(ZoneOffset.UTC);
			redisUtil.zAdd(followerTimelineKey, String.valueOf(post.getId()), score);
		}

		redisUtil.zRemRangeByRank(followerTimelineKey, TIMELINE_CACHE_LIMIT);
		log.info("Fan-out complete for FOLLOW_CREATED: follower={}, followee={}", followerId, followeeId);
	}

	private void handleFollowDeleted(TimelineFanoutMessage message) {
		Long followerId = message.getFollowerId();
		Long followeeId = message.getFolloweeId();

		List<Long> postIdsToRemove = postRepository.findAllIdsByAuthorId(followeeId);
		if (postIdsToRemove.isEmpty()) {
			return;
		}

		String followerTimelineKey = TIMELINE_ZSET_PREFIX + followerId;
		redisUtil.zRem(followerTimelineKey, postIdsToRemove.stream().map(String::valueOf).toArray(String[]::new));
		log.info("Fan-out complete for UNFOLLOW_CREATED: follower={}, followee={}", followerId, followeeId);
	}
}
