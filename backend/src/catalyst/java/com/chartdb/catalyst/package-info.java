package com.chartdb.catalyst;

/**
 * Zoho Catalyst Integration Package.
 * 
 * This package contains configuration classes for deploying ChartDB
 * to Zoho Catalyst serverless platform.
 * 
 * Components:
 * - CatalystCacheConfig: In-memory cache for Catalyst environment
 * - CatalystDataStoreConfig: Database configuration for Catalyst
 * 
 * Note: The actual Catalyst Advanced I/O handler (MainHandler) should be
 * created separately when packaging for Catalyst deployment, as it requires
 * the Catalyst SDK which is only available in the Catalyst runtime environment.
 * 
 * For deployment, create a separate MainHandler.java that:
 * 1. Implements com.zc.component.advancedio.ZCAdvancedIO
 * 2. Bootstraps the Spring Boot application
 * 3. Forwards requests to Spring's DispatcherServlet
 * 
 * See CATALYST_DEPLOYMENT_GUIDE.md for detailed instructions.
 */
