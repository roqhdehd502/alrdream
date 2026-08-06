package com.alrdream.domain.ai.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FreeTierSettingRepository extends JpaRepository<FreeTierSetting, UUID> {

	List<FreeTierSetting> findAllByOrderByCreatedAtAsc();
}
