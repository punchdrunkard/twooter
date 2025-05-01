package xyz.twooter.common.storage;

public interface StorageService {
	String generateUploadUrl(String objectPath, String contentType);
}
