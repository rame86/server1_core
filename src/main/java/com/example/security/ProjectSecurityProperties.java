package com.example.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "project.security")
@Getter @Setter
public class ProjectSecurityProperties {
    private boolean enabled;
}