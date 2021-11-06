package link.bosswang.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class DefaultMvcConfig implements WebMvcConfigurer {
	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(new StringHttpMessageConverter());
		converters.add(new MappingJackson2HttpMessageConverter());
		System.out.println("Hello World");
	}
}
