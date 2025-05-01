package xyz.twooter.media.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import xyz.twooter.media.application.MediaService;
import xyz.twooter.media.presentation.dto.request.SignedUrlResponse;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

	private final MediaService mediaService;

	// 파일 업로드 URL 요청
	@GetMapping("/upload-url")
	public ResponseEntity<SignedUrlResponse> getUploadUrl(@RequestParam String filename,
		@RequestParam String contentType) {
		SignedUrlResponse response = mediaService.generateUploadUrl(filename, contentType);
		return ResponseEntity.ok(response);
	}

	// 업로드 완료 알림
}
