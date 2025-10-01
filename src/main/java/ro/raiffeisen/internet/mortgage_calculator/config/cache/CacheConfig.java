package ro.raiffeisen.internet.mortgage_calculator.config.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    @Override
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("mortgageCalculatorCache");
    }
}