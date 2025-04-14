package xyz.twooter.common.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import xyz.twooter.support.IntegrationTestSupport;

class RedisUtilTest extends IntegrationTestSupport {

	@Autowired
	private RedisUtil redisUtil;

	private final String TEST_KEY = "test:key";
	private final String TEST_VALUE = "test-value";
	private final String TEST_HASH_KEY = "test:hash";

	@BeforeEach
	void setUp() {
		// 각 테스트 전에 테스트에 사용할 키를 삭제하여 테스트 환경 초기화
		redisUtil.delete(TEST_KEY);
		redisUtil.delete(TEST_HASH_KEY);
	}

	@AfterEach
	void tearDown() {
		// 각 테스트 후에 테스트에 사용한 키를 삭제하여 정리
		redisUtil.delete(TEST_KEY);
		redisUtil.delete(TEST_HASH_KEY);
	}

	@Test
	@DisplayName("문자열 값을 저장하고 조회할 수 있는지 검증한다")
	void shouldStoreAndRetrieveStringValueWhenUsingSetAndGet() {
		// given

		// when
		redisUtil.set(TEST_KEY, TEST_VALUE);
		Object retrievedValue = redisUtil.get(TEST_KEY);

		// then
		assertEquals(TEST_VALUE, retrievedValue);
	}

	@Test
	@DisplayName("만료 시간을 설정하여 값이 자동으로 만료되는지 검증한다")
	void shouldExpireValueWhenTimeoutIsSet() throws Exception {
		// given
		long timeoutMs = 1000; // 1초

		// when
		redisUtil.set(TEST_KEY, TEST_VALUE, timeoutMs, TimeUnit.MILLISECONDS);

		// 만료 전 확인
		Object valueBeforeExpiration = redisUtil.get(TEST_KEY);

		// 만료 시간 이후까지 대기
		Thread.sleep(1500); // 1.5초 대기

		// 만료 후 확인
		Object valueAfterExpiration = redisUtil.get(TEST_KEY);

		// then
		assertEquals(TEST_VALUE, valueBeforeExpiration);
		assertNull(valueAfterExpiration);
	}

	@Test
	@DisplayName("키 존재 여부 확인 기능이 올바르게 동작하는지 검증한다")
	void shouldCheckKeyExistenceCorrectlyWhenUsingHasKey() {
		// given

		// when
		boolean beforeSet = redisUtil.hasKey(TEST_KEY);
		redisUtil.set(TEST_KEY, TEST_VALUE);
		boolean afterSet = redisUtil.hasKey(TEST_KEY);

		// then
		assertFalse(beforeSet);
		assertTrue(afterSet);
	}

	@Test
	@DisplayName("키 삭제 기능이 올바르게 동작하는지 검증한다")
	void shouldDeleteKeyCorrectlyWhenUsingDelete() {
		// given
		redisUtil.set(TEST_KEY, TEST_VALUE);

		// when
		boolean hasKeyBeforeDelete = redisUtil.hasKey(TEST_KEY);
		boolean deleteResult = redisUtil.delete(TEST_KEY);
		boolean hasKeyAfterDelete = redisUtil.hasKey(TEST_KEY);

		// then
		assertTrue(hasKeyBeforeDelete);
		assertTrue(deleteResult);
		assertFalse(hasKeyAfterDelete);
	}

	@Test
	@DisplayName("여러 키를 한번에 삭제할 수 있는지 검증한다")
	void shouldDeleteMultipleKeysWhenUsingBulkDelete() {
		// given
		String testKey1 = TEST_KEY + ":1";
		String testKey2 = TEST_KEY + ":2";

		redisUtil.set(testKey1, "value1");
		redisUtil.set(testKey2, "value2");

		// when
		long deletedCount = redisUtil.delete(Arrays.asList(testKey1, testKey2));

		// then
		assertEquals(2, deletedCount);
		assertFalse(redisUtil.hasKey(testKey1));
		assertFalse(redisUtil.hasKey(testKey2));
	}

	@Test
	@DisplayName("해시 데이터 구조 작업이 올바르게 동작하는지 검증한다")
	void shouldManageHashDataCorrectlyWhenUsingHashOperations() {
		// given
		String field1 = "field1";
		String value1 = "value1";
		String field2 = "field2";
		String value2 = "value2";

		// when - 개별 필드 저장
		redisUtil.hSet(TEST_HASH_KEY, field1, value1);
		Object retrievedValue1 = redisUtil.hGet(TEST_HASH_KEY, field1);

		// 여러 필드 저장
		Map<String, Object> map = new HashMap<>();
		map.put(field1, value1);
		map.put(field2, value2);
		redisUtil.hSetAll(TEST_HASH_KEY, map);

		// 전체 해시 맵 조회
		Map<Object, Object> retrievedMap = redisUtil.hGetAll(TEST_HASH_KEY);

		// 필드 존재 여부 확인
		boolean hasField = redisUtil.hHasKey(TEST_HASH_KEY, field1);
		boolean hasNonExistentField = redisUtil.hHasKey(TEST_HASH_KEY, "non-existent");

		// 필드 삭제
		long deletedFields = redisUtil.hDelete(TEST_HASH_KEY, field1);
		boolean fieldExistsAfterDelete = redisUtil.hHasKey(TEST_HASH_KEY, field1);

		// then
		assertEquals(value1, retrievedValue1);
		assertEquals(2, retrievedMap.size());
		assertEquals(value1, retrievedMap.get(field1));
		assertEquals(value2, retrievedMap.get(field2));
		assertTrue(hasField);
		assertFalse(hasNonExistentField);
		assertEquals(1, deletedFields);
		assertFalse(fieldExistsAfterDelete);
	}

	@Test
	@DisplayName("패턴으로 키를 조회할 수 있는지 검증한다")
	void shouldFindKeysByPatternWhenUsingKeysMethod() {
		// given
		String keyPrefix = "test:pattern:";
		String key1 = keyPrefix + "1";
		String key2 = keyPrefix + "2";

		redisUtil.set(key1, "value1");
		redisUtil.set(key2, "value2");

		// when
		Set<String> keys = redisUtil.keys(keyPrefix + "*");

		// then
		assertEquals(2, keys.size());
		assertTrue(keys.contains(key1));
		assertTrue(keys.contains(key2));

		// 정리
		redisUtil.delete(key1);
		redisUtil.delete(key2);
	}

	@Test
	@DisplayName("타입 변환 메서드가 올바르게 동작하는지 검증한다")
	void shouldCastValueCorrectlyWhenUsingGetWithType() {
		// given
		Integer intValue = 123;
		redisUtil.set(TEST_KEY, intValue);

		// when
		Integer retrievedIntValue = redisUtil.get(TEST_KEY, Integer.class);

		// then
		assertEquals(intValue, retrievedIntValue);
	}

	@Test
	@DisplayName("만료 시간 관리 메서드가 올바르게 동작하는지 검증한다")
	void shouldManageExpirationCorrectlyWhenUsingExpireMethods() {
		// given
		redisUtil.set(TEST_KEY, TEST_VALUE);

		// when
		boolean expireResult = redisUtil.expire(TEST_KEY, 10, TimeUnit.SECONDS);
		long remainingTime = redisUtil.getExpire(TEST_KEY, TimeUnit.SECONDS);

		// then
		assertTrue(expireResult);
		assertTrue(remainingTime > 0 && remainingTime <= 10);
	}

}
