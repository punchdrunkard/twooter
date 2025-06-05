package xyz.twooter.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import xyz.twooter.auth.application.AuthService;
import xyz.twooter.auth.presentation.AuthController;
import xyz.twooter.config.TestSecurityConfiguration;
import xyz.twooter.media.application.MediaService;
import xyz.twooter.media.presentation.MediaController;
import xyz.twooter.post.application.PostService;
import xyz.twooter.post.application.TimelineService;
import xyz.twooter.post.presentation.PostController;
import xyz.twooter.post.presentation.TimelineController;

@WebMvcTest(controllers = {
	// TODO 사용할 컨트롤러
	AuthController.class,
	PostController.class,
	MediaController.class,
	TimelineController.class
})
@Import(TestSecurityConfiguration.class)
public abstract class ControllerTestSupport {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@MockitoBean
	protected AuthService authService;

	@MockitoBean
	protected PostService postService;

	@MockitoBean
	protected TimelineService timelineService;

	@MockitoBean
	protected MediaService mediaService;
}
