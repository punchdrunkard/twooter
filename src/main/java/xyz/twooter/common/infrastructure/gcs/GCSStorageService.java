package xyz.twooter.common.infrastructure.gcs;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;

import lombok.RequiredArgsConstructor;
import xyz.twooter.common.storage.StorageService;

@Profile("prod")
@Service
@RequiredArgsConstructor
public class GCSStorageService implements StorageService {

	@Value("${gcp.project-id}")
	private String projectId;

	@Value("${gcp.storage.bucket}")
	private String bucket;

	private final Storage storage;

	@Override
	public String generateUploadUrl(String objectPath, String contentType) {
		BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectPath)
			.setContentType(contentType)
			.build();

		URL signedUrl = storage.signUrl(
			blobInfo,
			10, TimeUnit.MINUTES,
			Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
			Storage.SignUrlOption.withV4Signature(),
			Storage.SignUrlOption.withContentType()
		);

		return signedUrl.toString();
	}
}
