package com.tracker.gateway;

import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ApiGatewayApplicationTests {

	// RateLimitConfig's real beans open a live connection to Redis at startup
	// (bucket4jRedisConnection eagerly calls RedisClient.connect(...)), which does not
	// resolve outside docker-compose. Mocking them here keeps this context-load test
	// hermetic without needing a real/embedded Redis.
	@MockBean
	private RedisClient redisClient;

	@MockBean
	private StatefulRedisConnection<String, byte[]> redisConnection;

	@MockBean
	private AsyncProxyManager<String> asyncProxyManager;

	@MockBean
	private ProxyManager<String> proxyManager;

	@Test
	void contextLoads() {
	}

}
