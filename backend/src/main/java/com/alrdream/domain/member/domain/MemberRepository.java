package com.alrdream.domain.member.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {

	Optional<Member> findByEmail(String email);

	Optional<Member> findByProviderAndProviderId(AuthProvider provider, String providerId);

	boolean existsByEmail(String email);
}
