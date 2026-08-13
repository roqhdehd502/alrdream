package com.alrdream.domain.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.alrdream.domain.ai.domain.FreeTierSetting;
import com.alrdream.domain.ai.domain.UsageQuota;
import com.alrdream.domain.ai.domain.UsageQuotaRepository;
import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.global.error.TooManyRequestsException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link UsageQuotaService} 단위 테스트 — advisory lock을 거는 네이티브 쿼리 호출은 Mockito로 무해화하고
 * (실제 락 동작 자체는 DB가 필요해 여기서 검증하지 않는다), 한도 계산/차감 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class UsageQuotaServiceTest {

	@Mock
	private UsageQuotaRepository usageQuotaRepository;
	@Mock
	private MemberRepository memberRepository;
	@Mock
	private FreeTierSettingService freeTierSettingService;
	@Mock
	private EntityManager entityManager;
	@Mock
	private Query lockQuery;

	private UsageQuotaService usageQuotaService;
	private UUID memberId;

	@BeforeEach
	void setUp() {
		usageQuotaService = new UsageQuotaService(usageQuotaRepository, memberRepository, freeTierSettingService);
		ReflectionTestUtils.setField(usageQuotaService, "entityManager", entityManager);
		memberId = UUID.randomUUID();

		// advisory lock 획득 쿼리 체이닝을 무해화 — 이 테스트는 락 자체(DB 필요)가 아니라 한도 계산 로직만 본다.
		lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(lockQuery);
		lenient().when(lockQuery.setParameter(anyString(), any())).thenReturn(lockQuery);
		lenient().when(lockQuery.getSingleResult()).thenReturn(null);
		lenient().when(usageQuotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
	}

	// FreeTierSetting은 값 보관용 엔티티라 Mockito로 흉내내기보다 실제 인스턴스를 만들어 쓰는 편이 더 안정적이다
	// (protected no-args 생성자라 리플렉션으로 우회한다 — ReflectionTestUtils는 필드는 채울 수 있어도 생성자를
	// 대신 호출해주지는 않는다).
	private FreeTierSetting settingsWith(int freeLimit, int proLimit) {
		try {
			var ctor = FreeTierSetting.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			FreeTierSetting setting = ctor.newInstance();
			setting.changeFreeMonthlyLimit(freeLimit);
			setting.changeProMonthlyLimit(proLimit);
			return setting;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void checkAndIncrement_이번_달_row가_없으면_새로_만들고_1로_증가() {
		Member member = Member.createLocal("user@example.com", "hashed");
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		when(freeTierSettingService.get()).thenReturn(settingsWith(1, 10));
		when(usageQuotaRepository.findByUserIdAndPeriod(eq(memberId), anyString())).thenReturn(Optional.empty());
		org.mockito.ArgumentCaptor<UsageQuota> captor = org.mockito.ArgumentCaptor.forClass(UsageQuota.class);

		usageQuotaService.checkAndIncrement(memberId);

		// save()는 새로 만든 시점(generationCount=0)에 호출되지만, 같은 객체 참조를 이후 increment()가 그대로
		// 변형하므로 검증 시점엔 이미 1이다 — 인자를 캡처해 "최종적으로 어떤 상태가 저장되어 있었는지"를 본다.
		org.mockito.Mockito.verify(usageQuotaRepository).save(captor.capture());
		assertThat(captor.getValue().getLimitCount()).isEqualTo(1); // FREE 한도로 생성됨
		assertThat(captor.getValue().getGenerationCount()).isEqualTo(1);
	}

	@Test
	void checkAndIncrement_FREE_회원은_기존_row의_generationCount를_증가() {
		Member member = Member.createLocal("user@example.com", "hashed");
		UsageQuota quota = UsageQuota.create(memberId, YearMonth.now().toString(), 1);
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		when(usageQuotaRepository.findByUserIdAndPeriod(eq(memberId), anyString())).thenReturn(Optional.of(quota));
		// 이미 row가 있으면 orElseGet의 한도 조회 로직 자체가 호출되지 않는다(관리자가 한도를 바꿔도 이번 달
		// 이미 생성된 row는 소급 반영되지 않는 기존 설계 — freeTierSettingService는 이 경로에서 쓰이지 않는다).

		usageQuotaService.checkAndIncrement(memberId);

		assertThat(quota.getGenerationCount()).isEqualTo(1);
	}

	@Test
	void checkAndIncrement_한도를_이미_채웠으면_예외() {
		Member member = Member.createLocal("user@example.com", "hashed");
		UsageQuota quota = UsageQuota.create(memberId, YearMonth.now().toString(), 1);
		quota.increment(); // 이미 1/1 사용
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		when(usageQuotaRepository.findByUserIdAndPeriod(eq(memberId), anyString())).thenReturn(Optional.of(quota));

		assertThatThrownBy(() -> usageQuotaService.checkAndIncrement(memberId))
				.isInstanceOf(TooManyRequestsException.class);
		assertThat(quota.getGenerationCount()).isEqualTo(1); // 더 증가하지 않음
	}

	@Test
	void checkAndIncrement_PRO_회원은_proMonthlyLimit_적용() {
		Member member = Member.createLocal("user@example.com", "hashed");
		member.changePlan(MemberPlan.PRO);
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		when(freeTierSettingService.get()).thenReturn(settingsWith(1, 10));
		when(usageQuotaRepository.findByUserIdAndPeriod(eq(memberId), anyString())).thenReturn(Optional.empty());

		usageQuotaService.checkAndIncrement(memberId);

		org.mockito.Mockito.verify(usageQuotaRepository).save(org.mockito.ArgumentMatchers.argThat(
				q -> q.getLimitCount() == 10)); // free(1)이 아니라 pro(10) 한도로 생성됨
	}

	@Test
	void getCurrent_이번_달_아직_생성_이력이_없으면_0으로_간주() {
		Member member = Member.createLocal("user@example.com", "hashed");
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		when(freeTierSettingService.get()).thenReturn(settingsWith(1, 10));
		when(usageQuotaRepository.findByUserIdAndPeriod(eq(memberId), anyString())).thenReturn(Optional.empty());

		UsageQuotaService.UsageQuotaSnapshot snapshot = usageQuotaService.getCurrent(memberId);

		assertThat(snapshot.generationCount()).isZero();
		assertThat(snapshot.limitCount()).isEqualTo(1);
		assertThat(snapshot.plan()).isEqualTo(MemberPlan.FREE);
	}

	@Test
	void getCurrent_row가_있으면_없으면_조회만_하고_새로_만들지_않음() {
		Member member = Member.createLocal("user@example.com", "hashed");
		UsageQuota quota = UsageQuota.create(memberId, YearMonth.now().toString(), 1);
		quota.increment();
		when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
		when(freeTierSettingService.get()).thenReturn(settingsWith(1, 10));
		when(usageQuotaRepository.findByUserIdAndPeriod(eq(memberId), anyString())).thenReturn(Optional.of(quota));

		UsageQuotaService.UsageQuotaSnapshot snapshot = usageQuotaService.getCurrent(memberId);

		assertThat(snapshot.generationCount()).isEqualTo(1);
		org.mockito.Mockito.verify(usageQuotaRepository, org.mockito.Mockito.never()).save(any());
	}

	// findByUserIdAndPeriod의 두 번째 인자(period)는 YearMonth.now() 기준이라 정확한 문자열을 미리 알 수 없어
	// eq() 대신 anyString()을 쓴다 — memberId만 정확히 일치하면 되므로 이 헬퍼로 가독성을 보존한다.
	private static UUID eq(UUID value) {
		return org.mockito.ArgumentMatchers.eq(value);
	}
}
