package com.alrdream.global.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM 기반 문자열 컬럼 암호화 컨버터.
 * [03] §5 암호화 대상 컬럼(survey_responses.answers, planning_versions.content, analysis_versions.content,
 * design_versions.content, subscriptions.billing_key)의 엔티티 필드에 {@code @Convert(converter = EncryptedStringConverter.class)}로 적용한다.
 * 암호문은 유효한 JSON이 아니므로 대응하는 DB 컬럼은 TEXT여야 한다 (JSONB 불가 — {@code V1__initial_schema.sql} 참고).
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final String base64Key;

    public EncryptedStringConverter(@Value("${app.security.encryption-key}") String base64Key) {
        this.base64Key = base64Key;
    }

    /**
     * 키 검증을 빈 생성자가 아니라 실제 변환 시점으로 미룬다 — 아직 이 컨버터를 쓰는 엔티티가 없는 상태에서
     * {@code DATA_ENCRYPTION_KEY}가 비어 있다는 이유만으로 애플리케이션 부팅 자체가 실패해서는 안 되기 때문이다.
     */
    private SecretKeySpec key() {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("app.security.encryption-key(DATA_ENCRYPTION_KEY)가 설정되지 않았습니다.");
        }
        return new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("필드 암호화에 실패했습니다.", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(dbData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("필드 복호화에 실패했습니다.", e);
        }
    }
}
