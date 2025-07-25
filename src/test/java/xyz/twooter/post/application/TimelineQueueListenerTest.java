package xyz.twooter.post.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import xyz.twooter.common.infrastructure.redis.RedisUtil;
import xyz.twooter.member.domain.repository.FollowRepository;
import xyz.twooter.post.application.dto.TimelineFanoutMessage;
import xyz.twooter.post.domain.Post;
import xyz.twooter.post.domain.repository.PostRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimelineQueueListenerTest {

    @InjectMocks
    private TimelineQueueListener timelineQueueListener;

    @Mock
    private RedisUtil redisUtil;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private PostRepository postRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private void invokeFanout(TimelineFanoutMessage message) throws Exception {
        String messageJson = objectMapper.writeValueAsString(message);
        ReflectionTestUtils.invokeMethod(timelineQueueListener, "fanout", messageJson);
    }

    @BeforeEach
    void setUp() {
        timelineQueueListener.initializeHandlers();
    }

    @Nested
    @DisplayName("게시물 생성 이벤트(POST_CREATED) 처리")
    class HandlePostCreated {
        @Test
        @DisplayName("성공 - 작성자와 모든 팔로워의 타임라인에 포스트가 추가되어야 한다")
        void shouldAddPostToAuthorAndFollowersTimelinesWhenPostCreated() throws Exception {
            // given
            Long authorId = 1L;
            Long postId = 101L;
            LocalDateTime createdAt = LocalDateTime.now();
            List<Long> followerIds = List.of(2L, 3L, 4L);

            // Post 객체를 Mock으로 생성하여 필요한 값만 설정
            Post mockPost = mock(Post.class);
            when(mockPost.getId()).thenReturn(postId);
            when(mockPost.getAuthorId()).thenReturn(authorId);
            when(mockPost.getCreatedAt()).thenReturn(createdAt);

            TimelineFanoutMessage message = TimelineFanoutMessage.ofPostCreation(mockPost);

            when(followRepository.findAllFollowerIdsByFolloweeId(authorId)).thenReturn(followerIds);

            // when
            invokeFanout(message);

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisUtil, times(4)).zAdd(keyCaptor.capture(), eq(String.valueOf(postId)), anyDouble());

            List<String> expectedKeys = List.of("timeline:user:1", "timeline:user:2", "timeline:user:3", "timeline:user:4");
            assertThat(keyCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(expectedKeys);

            verify(redisUtil, times(4)).zRemRangeByRank(anyString(), anyInt());
        }
    }

    @Nested
    @DisplayName("게시물 삭제 이벤트(POST_DELETED) 처리")
    class HandlePostDeleted {
        @Test
        @DisplayName("성공 - 작성자와 모든 팔로워의 타임라인에서 포스트가 삭제되어야 한다")
        void shouldRemovePostFromAuthorAndFollowersTimelinesWhenPostDeleted() throws Exception {
            // given
            Long authorId = 1L;
            Long postId = 101L;
            List<Long> followerIds = List.of(2L, 3L);

            Post mockPost = mock(Post.class);
            when(mockPost.getId()).thenReturn(postId);
            when(mockPost.getAuthorId()).thenReturn(authorId);

            TimelineFanoutMessage message = TimelineFanoutMessage.ofPostDeletion(mockPost);

            when(followRepository.findAllFollowerIdsByFolloweeId(authorId)).thenReturn(followerIds);

            // when
            invokeFanout(message);

            // then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(redisUtil, times(3)).zRem(keyCaptor.capture(), eq(String.valueOf(postId)));

            List<String> expectedKeys = List.of("timeline:user:1", "timeline:user:2", "timeline:user:3");
            assertThat(keyCaptor.getAllValues()).containsExactlyInAnyOrderElementsOf(expectedKeys);
        }
    }

    @Nested
    @DisplayName("팔로우 이벤트(FOLLOW_CREATED) 처리")
    class HandleFollowCreated {
        @Test
        @DisplayName("성공 - 팔로워의 타임라인에 피팔로워의 최신 글들이 추가되어야 한다")
        void shouldAddFolloweePostsToFollowerTimelineWhenFollowCreated() throws Exception {
            // given
            Long followerId = 1L;
            Long followeeId = 2L;
            TimelineFanoutMessage message = TimelineFanoutMessage.ofFollowCreation(followerId, followeeId);

            Post mockPost1 = mock(Post.class);
            when(mockPost1.getId()).thenReturn(101L);
            when(mockPost1.getCreatedAt()).thenReturn(LocalDateTime.now());

            Post mockPost2 = mock(Post.class);
            when(mockPost2.getId()).thenReturn(102L);
            when(mockPost2.getCreatedAt()).thenReturn(LocalDateTime.now().minusMinutes(1));

            List<Post> recentPosts = List.of(mockPost1, mockPost2);

            when(postRepository.findTop50ByAuthorIdAndIsDeletedFalseAndRepostOfIdIsNullOrderByIdDesc(followeeId))
                    .thenReturn(recentPosts);

            // when
            invokeFanout(message);

            // then
            String expectedKey = "timeline:user:" + followerId;
            verify(redisUtil, times(1)).zAdd(eq(expectedKey), eq("101"), anyDouble());
            verify(redisUtil, times(1)).zAdd(eq(expectedKey), eq("102"), anyDouble());
            verify(redisUtil, times(1)).zRemRangeByRank(eq(expectedKey), anyInt());
        }
    }

    @Nested
    @DisplayName("언팔로우 이벤트(UNFOLLOW_CREATED) 처리")
    class HandleUnfollowCreated {
        @Test
        @DisplayName("성공 - 팔로워의 타임라인에서 피팔로워의 모든 글이 삭제되어야 한다")
        void shouldRemoveFolloweePostsFromFollowerTimelineWhenUnfollowed() throws Exception {
            // given
            Long followerId = 1L;
            Long followeeId = 2L;
            TimelineFanoutMessage message = TimelineFanoutMessage.ofFollowDeletion(followerId, followeeId);
            List<Long> postIdsToRemove = List.of(101L, 102L, 103L);

            when(postRepository.findAllIdsByAuthorId(followeeId)).thenReturn(postIdsToRemove);

            // when
            invokeFanout(message);

            // then
            String expectedKey = "timeline:user:" + followerId;
            String[] expectedIdsToRemove = {"101", "102", "103"};
            verify(redisUtil, times(1)).zRem(eq(expectedKey), eq(expectedIdsToRemove));
        }
    }
}
