package com.alrdream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Phase 19 — ProGrantExpirationScheduler(쿠폰/관리자 지급 Pro 만료 처리)가 이 코드베이스의 첫 @Scheduled
// 잡이라 @EnableScheduling을 여기서 처음 켠다.
@EnableScheduling
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
