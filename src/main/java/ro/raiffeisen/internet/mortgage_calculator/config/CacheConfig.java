package ro.raiffeisen.internet.mortgage_calculator.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for mortgage calculator.
 * Uses Caffeine as the cache provider for high-performance in-memory caching.
 * 
 * Cache strategy: Only cache final calculation results.
 * All intermediate data (products, rates, districts, etc.) are fetched fresh on each calculation.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String MORTGAGE_CALCULATION_CACHE = "mortgageCalculations";

    /**
     * Configure Caffeine cache manager with specific settings.
     * 
     * Single Cache Strategy:
     * - mortgageCalculations: 24 hours TTL, max 1000 entries - caches complete calculation results
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                MORTGAGE_CALCULATION_CACHE
        );

        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * Caffeine cache builder configuration.
     * TTL: 24 hours (1 day) - maximizes cache hit rate while ensuring daily refresh
     * Max Size: 1000 entries - sufficient for typical daily load
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)    // 24 hours (1 day) TTL
                .maximumSize(1000)                        // Max 1000 entries
                .recordStats();                           // Enable cache statistics for monitoring
    }

}

