package com.alrdream.domain.member.infrastructure;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleIdTokenVerifierAdapter implements OAuthIdTokenVerifier {

	private final GoogleIdTokenVerifier verifier;

	public GoogleIdTokenVerifierAdapter(@Value("${app.oauth.google.client-id}") String clientId) {
		this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
				.setAudience(Collections.singletonList(clientId))
				.build();
	}

	@Override
	public OAuthUserInfo verify(String idToken) {
		try {
			GoogleIdToken googleIdToken = verifier.verify(idToken);
			if (googleIdToken == null) {
				throw new IllegalArgumentException("유효하지 않은 Google ID 토큰입니다.");
			}
			GoogleIdToken.Payload payload = googleIdToken.getPayload();
			return new OAuthUserInfo(payload.getSubject(), payload.getEmail());
		} catch (GeneralSecurityException | IOException e) {
			throw new IllegalArgumentException("Google ID 토큰 검증에 실패했습니다.", e);
		}
	}
}
