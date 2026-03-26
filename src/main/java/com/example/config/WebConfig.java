package com.example.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.common.resolver.LoginUserArgumentResolver;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final LoginUserArgumentResolver loginUserArgumentResolver;

	@Value("${file.upload.dir}")
	private String fileUploadDir;

	// 스프링이 WebClient를 주입해줄 수 있게 빈으로 등록.
	@Bean
	public WebClient webClient(WebClient.Builder builder) {
		return builder.build();
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(loginUserArgumentResolver);
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String uploadPath = fileUploadDir;
		if (!uploadPath.endsWith("/")) {
			uploadPath += "/";
		}

		// /images/core/** 요청을 로컬 업로드 디렉토리로 매핑
		registry.addResourceHandler("/images/core/**")
		.addResourceLocations("file:///" + uploadPath);
	}
}