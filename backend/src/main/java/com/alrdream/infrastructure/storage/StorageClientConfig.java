package com.alrdream.infrastructure.storage;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** [03] §4-6 — Supabase Storage는 S3 호환 API를 제공하므로 AWS SDK v2의 {@code S3Client}를 그대로 사용한다. */
@Configuration
public class StorageClientConfig {

	@Value("${app.storage.supabase.endpoint}")
	private String endpoint;

	@Value("${app.storage.supabase.region}")
	private String region;

	@Value("${app.storage.supabase.access-key}")
	private String accessKey;

	@Value("${app.storage.supabase.secret-key}")
	private String secretKey;

	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
				.endpointOverride(URI.create(endpoint))
				.region(Region.of(region))
				.credentialsProvider(credentialsProvider())
				// Supabase Storage는 버킷을 서브도메인이 아닌 경로로 구분하는 path-style 주소만 지원한다.
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
				.build();
	}

	@Bean
	public S3Presigner s3Presigner() {
		return S3Presigner.builder()
				.endpointOverride(URI.create(endpoint))
				.region(Region.of(region))
				.credentialsProvider(credentialsProvider())
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
				.build();
	}

	private StaticCredentialsProvider credentialsProvider() {
		return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
	}
}
