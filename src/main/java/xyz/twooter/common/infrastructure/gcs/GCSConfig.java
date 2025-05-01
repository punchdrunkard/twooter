package xyz.twooter.common.infrastructure.gcs;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

@Configuration
@Profile("prod")
public class GCSConfig {

	@Value("${gcp.project-id}")
	private String projectId;

	@Bean
	public Storage storage() throws IOException {
		GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
		return StorageOptions.newBuilder()
			.setProjectId(projectId)
			.setCredentials(credentials)
			.build()
			.getService();
	}
}
