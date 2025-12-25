package com.chartdb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Configuration for serving the frontend SPA from Spring Boot.
 * All non-API routes will serve index.html for client-side routing.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static resources
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // If the resource exists and is readable, return it
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // For SPA routing: return index.html for non-API, non-ws paths
                        // Skip API and WebSocket paths
                        if (resourcePath.startsWith("api/") || 
                            resourcePath.startsWith("ws/") ||
                            resourcePath.startsWith("actuator/")) {
                            return null;
                        }
                        
                        // Return index.html for SPA client-side routing
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
