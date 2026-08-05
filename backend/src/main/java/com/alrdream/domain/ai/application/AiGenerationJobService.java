package com.alrdream.domain.ai.application;

import com.alrdream.domain.ai.domain.AiGenerationJob;
import com.alrdream.domain.ai.domain.AiGenerationJobRepository;
import com.alrdream.domain.ai.domain.AiTargetType;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * [03] §4-3, §4-4 — AI 생성 Job의 생성과 상태 전이(PENDING → PROCESSING → COMPLETED|FAILED)를 담당하는 공용 인프라.
 * 실제 프롬프트 구성, LLM 호출, 생성 결과 저장은 기획/분석/설계 각 도메인이 {@code task}로 넘기고, 이 서비스는
 * Job 생명주기와 Virtual Thread 비동기 실행만 책임진다. AI 생성은 항상 이 {@link #submit}을 거치므로
 * ([01] 13번) FREE 티어 quota 체크도 여기서 공통 처리한다 — 각 도메인이 개별적으로 호출을 잊을 위험을 없앤다.
 */
@Service
public class AiGenerationJobService {

	private static final Logger log = LoggerFactory.getLogger(AiGenerationJobService.class);
	private static final int ERROR_MESSAGE_MAX_LENGTH = 1000;

	private final AiGenerationJobRepository aiGenerationJobRepository;
	private final UsageQuotaService usageQuotaService;
	private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

	public AiGenerationJobService(AiGenerationJobRepository aiGenerationJobRepository, UsageQuotaService usageQuotaService) {
		this.aiGenerationJobRepository = aiGenerationJobRepository;
		this.usageQuotaService = usageQuotaService;
	}

	/**
	 * quota를 체크/차감한 뒤 Job을 PENDING 상태로 즉시 생성해 반환하고, {@code task}는 현재 트랜잭션이 커밋된
	 * 뒤에야 Virtual Thread에서 실행한다 — 커밋 전에 비동기 스레드가 먼저 Job을 조회하면 아직 보이지 않는
	 * 레코드를 찾게 되는 문제를 막는다. quota 초과 시 {@link com.alrdream.global.error.TooManyRequestsException}이
	 * 발생하며 Job은 생성되지 않는다.
	 */
	@Transactional
	public AiGenerationJob submit(UUID userId, AiTargetType targetType, UUID targetId, Runnable task) {
		usageQuotaService.checkAndIncrement(userId);
		AiGenerationJob job = aiGenerationJobRepository.save(AiGenerationJob.create(userId, targetType, targetId));
		UUID jobId = job.getId();
		runAfterCommit(() -> virtualThreadExecutor.execute(() -> runTask(jobId, task)));
		return job;
	}

	@Transactional(readOnly = true)
	public AiGenerationJob getOwned(UUID jobId, UUID userId) {
		return aiGenerationJobRepository.findByIdAndUserId(jobId, userId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 작업입니다."));
	}

	private void runTask(UUID jobId, Runnable task) {
		updateStatus(jobId, AiGenerationJob::markProcessing);
		try {
			task.run();
			updateStatus(jobId, AiGenerationJob::markCompleted);
		} catch (Exception e) {
			log.error("AI 생성 Job 처리에 실패했습니다: jobId={}", jobId, e);
			updateStatus(jobId, job -> job.markFailed(truncate(e.getMessage())));
		}
	}

	// Job은 생성 후 이 백그라운드 작업 하나만 갱신하는 단일 writer라, find→mutate→save를 하나의 트랜잭션으로
	// 묶지 않아도 경합이 없다 — 각 리포지토리 호출은 스스로 트랜잭션 경계를 가진다(SimpleJpaRepository).
	private void updateStatus(UUID jobId, Consumer<AiGenerationJob> mutator) {
		aiGenerationJobRepository.findById(jobId).ifPresent(job -> {
			mutator.accept(job);
			aiGenerationJobRepository.save(job);
		});
	}

	private void runAfterCommit(Runnable runnable) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					runnable.run();
				}
			});
		} else {
			runnable.run();
		}
	}

	private String truncate(String message) {
		if (message == null || message.isBlank()) {
			return "생성 중 알 수 없는 오류가 발생했습니다.";
		}
		return message.length() > ERROR_MESSAGE_MAX_LENGTH ? message.substring(0, ERROR_MESSAGE_MAX_LENGTH) : message;
	}
}
