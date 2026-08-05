package com.alrdream.domain.workspace.domain;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/** Querydsl로 소유자 필터 + 이름 검색 + 정렬 + 페이징을 한 쿼리로 조합한다 ([03] §4-1 Querydsl 사용 방침). */
@Repository
public class WorkspaceQueryRepositoryImpl implements WorkspaceQueryRepository {

	private final JPAQueryFactory queryFactory;

	public WorkspaceQueryRepositoryImpl(JPAQueryFactory queryFactory) {
		this.queryFactory = queryFactory;
	}

	@Override
	public Page<Workspace> search(UUID userId, String keyword, Pageable pageable) {
		QWorkspace workspace = QWorkspace.workspace;

		BooleanBuilder where = new BooleanBuilder()
				.and(workspace.userId.eq(userId))
				.and(workspace.deletedAt.isNull());
		if (StringUtils.hasText(keyword)) {
			where.and(workspace.name.containsIgnoreCase(keyword));
		}

		List<Workspace> content = queryFactory
				.selectFrom(workspace)
				.where(where)
				.orderBy(toOrderSpecifiers(pageable.getSort(), workspace))
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize())
				.fetch();

		Long total = queryFactory
				.select(workspace.count())
				.from(workspace)
				.where(where)
				.fetchOne();

		return new PageImpl<>(content, pageable, total == null ? 0 : total);
	}

	private OrderSpecifier<?>[] toOrderSpecifiers(Sort sort, QWorkspace workspace) {
		if (sort.isUnsorted()) {
			return new OrderSpecifier<?>[] {workspace.createdAt.desc()};
		}
		return sort.stream()
				.map(order -> toOrderSpecifier(order, workspace))
				.toArray(OrderSpecifier<?>[]::new);
	}

	private OrderSpecifier<?> toOrderSpecifier(Sort.Order order, QWorkspace workspace) {
		ComparableExpressionBase<?> path = switch (order.getProperty()) {
			case "name" -> workspace.name;
			case "createdAt" -> workspace.createdAt;
			case "updatedAt" -> workspace.updatedAt;
			default -> throw new IllegalArgumentException("지원하지 않는 정렬 필드입니다: " + order.getProperty());
		};
		return order.isAscending() ? path.asc() : path.desc();
	}
}
