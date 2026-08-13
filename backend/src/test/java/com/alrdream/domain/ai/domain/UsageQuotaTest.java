package com.alrdream.domain.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsageQuotaTest {

	@Test
	void isExceeded_한도_미만이면_false() {
		UsageQuota quota = UsageQuota.create(UUID.randomUUID(), "2026-08", 1);

		assertThat(quota.isExceeded()).isFalse();
	}

	@Test
	void isExceeded_한도에_도달하면_true() {
		UsageQuota quota = UsageQuota.create(UUID.randomUUID(), "2026-08", 1);

		quota.increment();

		assertThat(quota.isExceeded()).isTrue();
	}

	@Test
	void isExceeded_한도가_0이면_생성_직후에도_true() {
		UsageQuota quota = UsageQuota.create(UUID.randomUUID(), "2026-08", 0);

		assertThat(quota.isExceeded()).isTrue();
	}

	@Test
	void increment_호출할수록_generationCount_증가() {
		UsageQuota quota = UsageQuota.create(UUID.randomUUID(), "2026-08", 10);

		quota.increment();
		quota.increment();

		assertThat(quota.getGenerationCount()).isEqualTo(2);
	}
}
