package com.alrdream.domain.member.infrastructure;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import java.net.URI;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Apple ID 토큰(identityToken)을 Apple JWKS({@code https://appleid.apple.com/auth/keys})로 검증한다. */
@Component
public class AppleIdTokenVerifierAdapter implements OAuthIdTokenVerifier {

	private static final String APPLE_ISSUER = "https://appleid.apple.com";
	private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";

	private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

	public AppleIdTokenVerifierAdapter(@Value("${app.oauth.apple.client-id}") String clientId) throws Exception {
		JWKSource<SecurityContext> keySource = JWKSourceBuilder.create(URI.create(APPLE_JWKS_URL).toURL()).build();
		JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);

		ConfigurableJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
		processor.setJWSKeySelector(keySelector);
		processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
				new JWTClaimsSet.Builder().issuer(APPLE_ISSUER).audience(clientId).build(),
				Set.of("sub", "email", "exp", "iat")));
		this.jwtProcessor = processor;
	}

	@Override
	public OAuthUserInfo verify(String idToken) {
		try {
			JWTClaimsSet claims = jwtProcessor.process(idToken, null);
			return new OAuthUserInfo(claims.getSubject(), claims.getStringClaim("email"));
		} catch (Exception e) {
			throw new IllegalArgumentException("Apple ID 토큰 검증에 실패했습니다.", e);
		}
	}
}
