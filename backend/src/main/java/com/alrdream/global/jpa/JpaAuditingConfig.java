package com.alrdream.global.jpa;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

	// Spring Data JPA Auditing의 기본 DateTimeProvider는 LocalDateTime을 생성하는데,
	// BaseEntity의 createdAt/updatedAt은 OffsetDateTime이라 변환이 실패한다 (지원 타입 목록에 OffsetDateTime 없음).
	@Bean
	public DateTimeProvider auditingDateTimeProvider() {
		return () -> Optional.of(OffsetDateTime.now());
	}
}
