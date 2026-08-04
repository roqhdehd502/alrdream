package com.alrdream.global.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.Getter;

/**
 * [03] §5 소프트 삭제 대상 테이블(workspaces, planning_versions, analysis_versions, design_versions)이 상속한다.
 * users/survey_definitions/survey_responses/documents/ai_generation_jobs/subscriptions/payment_history/usage_quotas는
 * 소프트 삭제 대상이 아니므로 {@link BaseEntity}만 상속한다.
 */
@Getter
@MappedSuperclass
public abstract class SoftDeleteBaseEntity extends BaseEntity {

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markDeleted() {
        this.deletedAt = OffsetDateTime.now();
    }
}
