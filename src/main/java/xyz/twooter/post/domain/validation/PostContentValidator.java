package xyz.twooter.post.domain.validation;

import org.springframework.util.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import xyz.twooter.post.presentation.dto.request.PostCreateRequest;

public class PostContentValidator implements ConstraintValidator<ValidPostContent, PostCreateRequest> {
	@Override
	public boolean isValid(PostCreateRequest request, ConstraintValidatorContext context) {
		// Check if content has text
		boolean hasContent = StringUtils.hasText(request.getContent());

		// Check if media array has items
		boolean hasMedia = request.getMedia() != null && request.getMedia().length > 0;

		// Valid if at least one condition is true
		return hasContent || hasMedia;
	}
}
