package com.google.smarthome;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class SmarthomeApplicationTests {

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;


	@Test
	void contextLoads() {
	}

}
