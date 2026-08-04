package com.alrdream.global.flyway;

import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.stereotype.Component;

/**
 * flyway_schema_history 자체에 RLS를 켜는 작업을 Flyway가 추적하는 일반 마이그레이션(과거 V3)으로 실행하면,
 * Flyway가 같은 히스토리 테이블을 동시에 두 커넥션으로 다루다 자기 자신과 락이 걸려 무한 대기에 빠진다
 * (Supabase Session Pooler / 로컬 Testcontainers 양쪽에서 100% 재현 확인됨). migrate() 전체가 끝난 뒤
 * 실행되는 afterMigrate 콜백으로 옮겨 이 자기-데드락을 피한다. ENABLE ROW LEVEL SECURITY는 멱등이라
 * 재실행해도 안전하다 (이미 켜져 있으면 아무 일도 일어나지 않는다).
 */
@Component
public class EnableFlywaySchemaHistoryRlsCallback implements Callback {

	@Override
	public boolean supports(Event event, Context context) {
		return event == Event.AFTER_MIGRATE;
	}

	@Override
	public boolean canHandleInTransaction(Event event, Context context) {
		return true;
	}

	@Override
	public void handle(Event event, Context context) {
		try (Statement statement = context.getConnection().createStatement()) {
			statement.execute("ALTER TABLE flyway_schema_history ENABLE ROW LEVEL SECURITY");
		} catch (SQLException e) {
			throw new IllegalStateException("flyway_schema_history RLS 적용에 실패했습니다.", e);
		}
	}

	@Override
	public String getCallbackName() {
		return "enableFlywaySchemaHistoryRls";
	}
}
