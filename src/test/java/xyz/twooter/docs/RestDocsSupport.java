package xyz.twooter.docs;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import xyz.twooter.support.ControllerTestSupport;

@ExtendWith(RestDocumentationExtension.class)
@Disabled
public abstract class RestDocsSupport extends ControllerTestSupport {

	@BeforeEach
	void setUp(WebApplicationContext context, RestDocumentationContextProvider provider) {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.apply(documentationConfiguration(provider))
			.build();
	}
}
