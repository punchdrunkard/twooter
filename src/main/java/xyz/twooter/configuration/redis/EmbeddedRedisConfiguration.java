package xyz.twooter.configuration.redis;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import redis.embedded.RedisServer;

@Configuration
@Profile({"local", "test"}) // 로컬 및 테스트 환경에서만 활성화
public class EmbeddedRedisConfiguration {

	@Value("${spring.data.redis.port}")
	private int redisPort;

	private RedisServer redisServer;

	@PostConstruct
	public void startRedis() throws IOException {
		try {
			redisServer = new RedisServer(redisPort);
			redisServer.start();
		} catch (Exception e) {
			// 이미 해당 포트를 사용 중인 경우 (다른 Redis 인스턴스가 실행 중일 수 있음)
			// 포트를 사용할 수 있는지 확인하기 위한 로직 추가
			if (isRedisRunning()) {
				return; // 이미 실행 중이면 새로 시작하지 않음
			}
			throw e;
		}
	}

	@PreDestroy
	public void stopRedis() throws IOException {
		if (redisServer != null && redisServer.isActive()) {
			redisServer.stop();
		}
	}

	/**
	 * 현재 Redis가 실행 중인지 확인
	 */
	private boolean isRedisRunning() {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("localhost", redisPort), 100);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
