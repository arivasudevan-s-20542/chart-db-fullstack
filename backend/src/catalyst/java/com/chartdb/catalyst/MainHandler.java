package com.chartdb.catalyst;

import com.chartdb.ChartDbApplication;
import com.zc.component.advancedio.ZCAdvancedIO;
import com.zc.component.advancedio.ZCAdvancedIORequest;
import com.zc.component.advancedio.ZCAdvancedIOResponse;
import com.zc.component.advancedio.ZCFunction;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

/**
 * Zoho Catalyst Advanced I/O Handler for ChartDB Backend.
 * This class serves as the entry point for Catalyst function execution.
 */
public class MainHandler implements ZCAdvancedIO {
    
    private static final Logger LOGGER = Logger.getLogger(MainHandler.class.getName());
    private static ConfigurableApplicationContext springContext;
    private static boolean isInitialized = false;
    
    /**
     * Initialize Spring Boot application on cold start
     */
    private synchronized void initializeSpringBoot() {
        if (!isInitialized) {
            LOGGER.info("Initializing Spring Boot application for Catalyst...");
            try {
                // Set Catalyst profile
                System.setProperty("spring.profiles.active", "catalyst");
                
                // Start Spring Boot
                springContext = SpringApplication.run(ChartDbApplication.class);
                isInitialized = true;
                LOGGER.info("Spring Boot application initialized successfully");
            } catch (Exception e) {
                LOGGER.severe("Failed to initialize Spring Boot: " + e.getMessage());
                throw new RuntimeException("Spring Boot initialization failed", e);
            }
        }
    }
    
    @Override
    public void runner(ZCAdvancedIORequest request, ZCAdvancedIOResponse response) throws Exception {
        // Ensure Spring Boot is initialized
        initializeSpringBoot();
        
        try {
            // Get the Spring dispatcher servlet and forward the request
            HttpServletRequest httpRequest = request.getHttpServletRequest();
            HttpServletResponse httpResponse = response.getHttpServletResponse();
            
            // Set CORS headers for Catalyst
            httpResponse.setHeader("Access-Control-Allow-Origin", "*");
            httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
            httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
            httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
            
            // Handle OPTIONS preflight requests
            if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
                httpResponse.setStatus(HttpServletResponse.SC_OK);
                return;
            }
            
            // Forward to Spring MVC dispatcher
            springContext.getBean("dispatcherServlet", javax.servlet.Servlet.class)
                .service(httpRequest, httpResponse);
                
        } catch (Exception e) {
            LOGGER.severe("Error processing request: " + e.getMessage());
            response.getHttpServletResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getHttpServletResponse().getWriter().write("{\"error\": \"Internal server error\"}");
        }
    }
}
