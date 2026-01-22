package com.chartdb.catalyst;

import com.zc.component.cache.ZCCache;
import com.zc.component.cache.ZCCacheService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Catalyst Cache Configuration.
 * Integrates Zoho Catalyst Cache service with Spring Cache abstraction.
 */
@Configuration
@Profile("catalyst")
public class CatalystCacheConfig {
    
    private static final Logger LOGGER = Logger.getLogger(CatalystCacheConfig.class.getName());
    
    @Bean
    public CacheManager cacheManager() {
        return new CatalystCacheManager();
    }
    
    /**
     * Custom CacheManager that wraps Catalyst Cache service
     */
    public static class CatalystCacheManager implements CacheManager {
        
        @Override
        public Cache getCache(String name) {
            try {
                ZCCache zcCache = ZCCacheService.getInstance().getCache(name);
                return new CatalystCache(name, zcCache);
            } catch (Exception e) {
                LOGGER.warning("Failed to get Catalyst cache: " + name + " - " + e.getMessage());
                return null;
            }
        }
        
        @Override
        public Collection<String> getCacheNames() {
            return Collections.emptyList();
        }
    }
    
    /**
     * Spring Cache wrapper for Catalyst Cache
     */
    public static class CatalystCache implements Cache {
        
        private final String name;
        private final ZCCache zcCache;
        
        public CatalystCache(String name, ZCCache zcCache) {
            this.name = name;
            this.zcCache = zcCache;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public Object getNativeCache() {
            return zcCache;
        }
        
        @Override
        public ValueWrapper get(Object key) {
            try {
                String value = zcCache.get(key.toString());
                return value != null ? () -> value : null;
            } catch (Exception e) {
                return null;
            }
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Class<T> type) {
            try {
                String value = zcCache.get(key.toString());
                return (T) value;
            } catch (Exception e) {
                return null;
            }
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader) {
            try {
                String value = zcCache.get(key.toString());
                if (value == null) {
                    T loadedValue = valueLoader.call();
                    put(key, loadedValue);
                    return loadedValue;
                }
                return (T) value;
            } catch (Exception e) {
                try {
                    return valueLoader.call();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        
        @Override
        public void put(Object key, Object value) {
            try {
                if (value != null) {
                    zcCache.put(key.toString(), value.toString(), 3600); // 1 hour TTL
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to put value in cache: " + e.getMessage());
            }
        }
        
        @Override
        public void evict(Object key) {
            try {
                zcCache.delete(key.toString());
            } catch (Exception e) {
                LOGGER.warning("Failed to evict from cache: " + e.getMessage());
            }
        }
        
        @Override
        public void clear() {
            // Catalyst cache doesn't support clear all
            LOGGER.info("Clear all not supported in Catalyst cache");
        }
    }
}
