package xyz.twooter.common.storage;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile({"local", "test"})
@Service
public class MockStorageService implements StorageService {

	@Override
	public String generateUploadUrl(String objectPath, String contentType) {
		return "http://localhost:8080/upload/" + objectPath;
	}
}
