/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.config.annotation;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletConfig;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * Integration tests for view resolution with {@code @EnableWebMvc}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
class ViewResolutionIntegrationTests {

	private static final String EXPECTED_BODY = "<html><body>Hello, Java Café</body></html>";

	private static final boolean utf8Default = StandardCharsets.UTF_8.name().equals(System.getProperty("file.encoding"));


	@Nested
	class FreeMarkerTests {

		@Test
		void freemarkerWithInvalidConfig() {
			assertThatRuntimeException()
					.isThrownBy(() -> runTest(InvalidFreeMarkerWebConfig.class))
					.withMessageContaining("In addition to a FreeMarker view resolver ");
		}

		@Test
		void freemarkerWithDefaults() throws Exception {
			MockHttpServletResponse response = runTest(FreeMarkerWebConfig.class);
			assertThat(response.isCharset()).as("character encoding set in response").isTrue();
			if (utf8Default) {
				assertThat(response.getContentAsString()).isEqualTo(EXPECTED_BODY);
			}
			// Prior to Spring Framework 6.2, the charset is not updated in the Content-Type.
			// Thus, we expect ISO-8859-1 instead of UTF-8.
			assertThat(response.getCharacterEncoding()).isEqualTo("ISO-8859-1");
			assertThat(response.getContentType()).isEqualTo("text/html;charset=ISO-8859-1");
		}

		@Test  // gh-16629, gh-33071
		void freemarkerWithExistingViewResolver() throws Exception {
			MockHttpServletResponse response = runTest(ExistingViewResolverConfig.class);
			assertThat(response.isCharset()).as("character encoding set in response").isTrue();
			if (utf8Default) {
				assertThat(response.getContentAsString()).isEqualTo(EXPECTED_BODY);
			}
			// Prior to Spring Framework 6.2, the charset is not updated in the Content-Type.
			// Thus, we expect ISO-8859-1 instead of UTF-8.
			assertThat(response.getCharacterEncoding()).isEqualTo("ISO-8859-1");
			assertThat(response.getContentType()).isEqualTo("text/html;charset=ISO-8859-1");
		}

		@Test  // gh-33071
		void freemarkerWithExplicitDefaultEncoding() throws Exception {
			MockHttpServletResponse response = runTest(ExplicitDefaultEncodingConfig.class);
			assertThat(response.isCharset()).as("character encoding set in response").isTrue();
			assertThat(response.getContentAsString()).isEqualTo(EXPECTED_BODY);
			// Prior to Spring Framework 6.2, the charset is not updated in the Content-Type.
			// Thus, we expect ISO-8859-1 instead of UTF-8.
			assertThat(response.getCharacterEncoding()).isEqualTo("ISO-8859-1");
			assertThat(response.getContentType()).isEqualTo("text/html;charset=ISO-8859-1");
		}

		@Test  // gh-33071
		void freemarkerWithExplicitDefaultEncodingAndContentType() throws Exception {
			MockHttpServletResponse response = runTest(ExplicitDefaultEncodingAndContentTypeConfig.class);
			assertThat(response.isCharset()).as("character encoding set in response").isTrue();
			assertThat(response.getContentAsString()).isEqualTo(EXPECTED_BODY);
			// When the Content-Type is explicitly set on the view resolver, it should be used.
			assertThat(response.getCharacterEncoding()).isEqualTo("UTF-16");
			assertThat(response.getContentType()).isEqualTo("text/html;charset=UTF-16");
		}


		@Configuration
		static class InvalidFreeMarkerWebConfig extends WebMvcConfigurationSupport {

			@Override
			public void configureViewResolvers(ViewResolverRegistry registry) {
				registry.freeMarker();
			}
		}

		@Configuration
		static class FreeMarkerWebConfig extends AbstractWebConfig {

			@Override
			public void configureViewResolvers(ViewResolverRegistry registry) {
				registry.freeMarker();
			}

			@Bean
			public FreeMarkerConfigurer freeMarkerConfigurer() {
				FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
				configurer.setTemplateLoaderPath("/WEB-INF/");
				return configurer;
			}
		}

		@Configuration
		static class ExistingViewResolverConfig extends AbstractWebConfig {

			@Bean
			public FreeMarkerViewResolver freeMarkerViewResolver() {
				return new FreeMarkerViewResolver("", ".ftl");
			}

			@Bean
			public FreeMarkerConfigurer freeMarkerConfigurer() {
				FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
				configurer.setTemplateLoaderPath("/WEB-INF/");
				return configurer;
			}
		}

		@Configuration
		static class ExplicitDefaultEncodingConfig extends AbstractWebConfig {

			@Override
			public void configureViewResolvers(ViewResolverRegistry registry) {
				registry.freeMarker();
			}

			@Bean
			public FreeMarkerConfigurer freeMarkerConfigurer() {
				FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
				configurer.setTemplateLoaderPath("/WEB-INF/");
				configurer.setDefaultEncoding(StandardCharsets.UTF_8.name());
				return configurer;
			}
		}

		@Configuration
		static class ExplicitDefaultEncodingAndContentTypeConfig extends AbstractWebConfig {

			@Bean
			public FreeMarkerViewResolver freeMarkerViewResolver() {
				FreeMarkerViewResolver resolver = new FreeMarkerViewResolver("", ".ftl");
				resolver.setContentType("text/html;charset=UTF-16");
				return resolver;
			}

			@Bean
			public FreeMarkerConfigurer freeMarkerConfigurer() {
				FreeMarkerConfigurer configurer = new FreeMarkerConfigurer();
				configurer.setTemplateLoaderPath("/WEB-INF/");
				configurer.setDefaultEncoding(StandardCharsets.UTF_8.name());
				return configurer;
			}
		}
	}


	@Nested
	class GroovyMarkupTests {

		@Test
		void groovyMarkupInvalidConfig() {
			assertThatRuntimeException()
					.isThrownBy(() -> runTest(InvalidGroovyMarkupWebConfig.class))
					.withMessageContaining("In addition to a Groovy markup view resolver ");
		}

		@Test
		void groovyMarkup() throws Exception {
			MockHttpServletResponse response = runTest(GroovyMarkupWebConfig.class);
			if (utf8Default) {
				assertThat(response.getContentAsString()).isEqualTo(EXPECTED_BODY);
			}
		}


		@Configuration
		static class InvalidGroovyMarkupWebConfig extends WebMvcConfigurationSupport {

			@Override
			public void configureViewResolvers(ViewResolverRegistry registry) {
				registry.groovy();
			}
		}

		@Configuration
		static class GroovyMarkupWebConfig extends AbstractWebConfig {

			@Override
			public void configureViewResolvers(ViewResolverRegistry registry) {
				registry.groovy();
			}

			@Bean
			public GroovyMarkupConfigurer groovyMarkupConfigurer() {
				GroovyMarkupConfigurer configurer = new GroovyMarkupConfigurer();
				configurer.setResourceLoaderPath("/WEB-INF/");
				return configurer;
			}
		}
	}


	private static MockHttpServletResponse runTest(Class<?> configClass) throws Exception {
		String basePath = "org/springframework/web/servlet/config/annotation";
		MockServletContext servletContext = new MockServletContext(basePath);
		MockServletConfig servletConfig = new MockServletConfig(servletContext);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		MockHttpServletResponse response = new MockHttpServletResponse();

		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(configClass);
		context.setServletContext(servletContext);
		context.refresh();
		DispatcherServlet servlet = new DispatcherServlet(context);
		servlet.init(servletConfig);
		servlet.service(request, response);
		return response;
	}


	@Controller
	static class SampleController {

		@GetMapping
		String index(ModelMap model) {
			model.put("hello", "Hello");
			return "index";
		}
	}

	@EnableWebMvc
	abstract static class AbstractWebConfig implements WebMvcConfigurer {

		@Bean
		public SampleController sampleController() {
			return new SampleController();
		}
	}

}
