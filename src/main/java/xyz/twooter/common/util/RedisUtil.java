package xyz.twooter.common.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.twooter.common.error.BusinessException;
import xyz.twooter.common.error.ErrorCode;

/**
 * Redis 작업을 간소화하기 위한 유틸리티 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

	private final RedisTemplate<String, Object> redisTemplate;

	/**
	 * 키-값 쌍을 저장합니다.
	 *
	 * @param key   키
	 * @param value 값
	 */
	public void set(String key, Object value) {
		try {
			redisTemplate.opsForValue().set(key, value);
			log.debug("Successfully set key: {}", key);
		} catch (Exception e) {
			log.error("Failed to set key: {}", key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 키-값 쌍을 저장하고 만료 시간을 설정합니다.
	 *
	 * @param key     키
	 * @param value   값
	 * @param timeout 만료 시간
	 * @param unit    시간 단위
	 */
	public void set(String key, Object value, long timeout, TimeUnit unit) {
		try {
			redisTemplate.opsForValue().set(key, value, timeout, unit);
			log.debug("Successfully set key: {} with expiration: {} {}", key, timeout, unit);
		} catch (Exception e) {
			log.error("Failed to set key with expiration: {}", key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 키에 해당하는 값을 조회합니다.
	 *
	 * @param key 키
	 * @return 값 (없는 경우 null)
	 */
	public Object get(String key) {
		try {
			return redisTemplate.opsForValue().get(key);
		} catch (Exception e) {
			log.error("Failed to get key: {}", key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 키에 해당하는 값을 특정 타입으로 조회합니다.
	 *
	 * @param key   키
	 * @param clazz 반환 타입
	 * @return 값 (없는 경우 null)
	 */
	public <T> T get(String key, Class<T> clazz) {
		try {
			Object obj = redisTemplate.opsForValue().get(key);
			if (obj == null) {
				return null;
			}
			return clazz.cast(obj);
		} catch (ClassCastException e) {
			log.error("Failed to cast value for key: {} to type: {}", key, clazz.getName(), e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		} catch (Exception e) {
			log.error("Failed to get key with type: {}", key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 키의 존재 여부를 확인합니다.
	 *
	 * @param key 키
	 * @return 존재하면 true, 그렇지 않으면 false
	 */
	public boolean hasKey(String key) {
		try {
			return Boolean.TRUE.equals(redisTemplate.hasKey(key));
		} catch (Exception e) {
			log.error("Failed to check key existence: {}", key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 키를 삭제합니다.
	 *
	 * @param key 키
	 * @return 삭제 성공 시 true, 실패 시 false
	 */
	public boolean delete(String key) {
		try {
			return Boolean.TRUE.equals(redisTemplate.delete(key));
		} catch (Exception e) {
			log.error("Failed to delete key: {}", key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 여러 키를 한번에 삭제합니다.
	 *
	 * @param keys 삭제할 키 목록
	 * @return 삭제된 키 개수
	 */
	public long delete(List<String> keys) {
		try {
			return redisTemplate.delete(keys);
		} catch (Exception e) {
			log.error("Failed to delete keys: {}", keys, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 키의 만료 시간을 설정합니다.
	 *
	 * @param key     키
	 * @param timeout 만료 시간
	 * @param unit    시간 단위
	 * @return 성공 시 true, 실패 시 false
	 */
	public boolean expire(String key, long timeout, TimeUnit unit) {
		try {
			return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
		} catch (Exception e) {
			log.error("Failed to set expiration for key: {}", key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 키의 남은 만료 시간을 조회합니다.
	 *
	 * @param key  키
	 * @param unit 시간 단위
	 * @return 남은 만료 시간 (키가 없거나 만료 설정이 없으면 -1, 만료되었으면 -2)
	 */
	public long getExpire(String key, TimeUnit unit) {
		try {
			Long expireTime = redisTemplate.getExpire(key, unit);
			return expireTime != null ? expireTime : -1;
		} catch (Exception e) {
			log.error("Failed to get expiration for key: {}", key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * 패턴에 일치하는 모든 키를 조회합니다.
	 *
	 * @param pattern 키 패턴 (예: user:*)
	 * @return 일치하는 키 집합
	 */
	public Set<String> keys(String pattern) {
		try {
			return redisTemplate.keys(pattern);
		} catch (Exception e) {
			log.error("Failed to get keys with pattern: {}", pattern, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	// Hash 작업을 위한 메서드들

	/**
	 * Hash에 필드-값 쌍을 저장합니다.
	 *
	 * @param key   키
	 * @param field 필드
	 * @param value 값
	 */
	public void hSet(String key, String field, Object value) {
		try {
			redisTemplate.opsForHash().put(key, field, value);
		} catch (Exception e) {
			log.error("Failed to set hash field: {} for key: {}", field, key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Hash에 여러 필드-값 쌍을 한번에 저장합니다.
	 *
	 * @param key 키
	 * @param map 필드-값 맵
	 */
	public void hSetAll(String key, Map<String, Object> map) {
		try {
			redisTemplate.opsForHash().putAll(key, map);
		} catch (Exception e) {
			log.error("Failed to set multiple hash fields for key: {}", key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Hash에서 필드에 해당하는 값을 조회합니다.
	 *
	 * @param key   키
	 * @param field 필드
	 * @return 값
	 */
	public Object hGet(String key, String field) {
		try {
			return redisTemplate.opsForHash().get(key, field);
		} catch (Exception e) {
			log.error("Failed to get hash field: {} for key: {}", field, key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Hash에서 필드를 삭제합니다.
	 *
	 * @param key    키
	 * @param fields 삭제할 필드들
	 * @return 삭제된 필드 수
	 */
	public long hDelete(String key, Object... fields) {
		try {
			return redisTemplate.opsForHash().delete(key, fields);
		} catch (Exception e) {
			log.error("Failed to delete hash fields for key: {}", key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Hash의 모든 필드-값 쌍을 조회합니다.
	 *
	 * @param key 키
	 * @return 필드-값 맵
	 */
	public Map<Object, Object> hGetAll(String key) {
		try {
			return redisTemplate.opsForHash().entries(key);
		} catch (Exception e) {
			log.error("Failed to get all hash entries for key: {}", key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Hash에서 필드 존재 여부를 확인합니다.
	 *
	 * @param key   키
	 * @param field 필드
	 * @return 존재하면 true, 그렇지 않으면 false
	 */
	public boolean hHasKey(String key, String field) {
		try {
			return redisTemplate.opsForHash().hasKey(key, field);
		} catch (Exception e) {
			log.error("Failed to check hash field existence: {} for key: {}", field, key, e);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}
}
