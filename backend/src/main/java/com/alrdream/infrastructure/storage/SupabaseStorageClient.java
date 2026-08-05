package com.alrdream.infrastructure.storage;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Component
public class SupabaseStorageClient implements StorageClient {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final String bucket;

	public SupabaseStorageClient(
			S3Client s3Client, S3Presigner s3Presigner, @Value("${app.storage.supabase.bucket}") String bucket) {
		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;
		this.bucket = bucket;
	}

	@Override
	public void upload(String key, byte[] content, String contentType) {
		s3Client.putObject(
				PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
				RequestBody.fromBytes(content));
	}

	@Override
	public String generateDownloadUrl(String key, Duration ttl) {
		PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
				.signatureDuration(ttl)
				.getObjectRequest(builder -> builder.bucket(bucket).key(key))
				.build());
		return presigned.url().toString();
	}
}
