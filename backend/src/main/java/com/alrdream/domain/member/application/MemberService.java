package com.alrdream.domain.member.application;

import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;

	public MemberService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public Member getById(UUID memberId) {
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
	}

	/** [03] §2-1 Admin의 CS 대응용 사용자 조회 — 이메일 부분 일치 검색. */
	public Page<Member> search(String keyword, Pageable pageable) {
		return StringUtils.hasText(keyword)
				? memberRepository.findByEmailContainingIgnoreCase(keyword, pageable)
				: memberRepository.findAll(pageable);
	}
}
