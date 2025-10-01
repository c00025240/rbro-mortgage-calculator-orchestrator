package ro.raiffeisen.internet.mortgage_calculator.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static ro.raiffeisen.internet.mortgage_calculator.config.CacheConfig.*;

/**
 * Tests for cache configuration.
 * Verifies that the mortgage calculation cache is properly configured.
 */
@SpringBootTest
class CacheConfigTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void contextLoads() {
        assertThat(cacheManager).isNotNull();
    }

    @Test
    void mortgageCalculationCache_isConfigured() {
        assertThat(cacheManager.getCache(MORTGAGE_CALCULATION_CACHE)).isNotNull();
    }

    @Test
    void onlyMortgageCalculationCache_isConfigured() {
        assertThat(cacheManager.getCacheNames()).containsExactly(
                MORTGAGE_CALCULATION_CACHE
        );
    }
}

