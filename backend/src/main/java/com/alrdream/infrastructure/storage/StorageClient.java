package com.alrdream.infrastructure.storage;

import java.time.Duration;

/** [03] §4-6 Supabase Storage(S3 호환) 업로드/서명 URL 발급. */
public interface StorageClient {

	/** {@code key}에 {@code content}를 업로드한다. 이미 같은 key가 있으면 덮어쓴다. */
	void upload(String key, byte[] content, String contentType);

	/** {@code key}로 업로드된 오브젝트를 내려받을 수 있는 서명 URL을 {@code ttl} 동안 유효하게 발급한다. */
	String generateDownloadUrl(String key, Duration ttl);
}
