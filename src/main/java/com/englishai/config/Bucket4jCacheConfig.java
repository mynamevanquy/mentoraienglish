package com.englishai.config;

import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider;
import com.giffing.bucket4j.spring.boot.starter.config.cache.SyncCacheResolver;
import com.giffing.bucket4j.spring.boot.starter.config.cache.jcache.JCacheCacheResolver;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

/**
 * Configuration for Bucket4j rate-limiting cache using Caffeine JCache provider.
 * Provides a {@link SyncCacheResolver} bean that the bucket4j-spring-boot-starter
 * uses to resolve the "buckets" cache defined in application.yml.
 */
@Configuration
@EnableCaching
public class Bucket4jCacheConfig {

    @Bean
    @Primary
    public SyncCacheResolver bucket4jCacheResolver() {
        // Obtain Caffeine JCache provider
        var cachingProvider = Caching.getCachingProvider(CaffeineCachingProvider.class.getName());
        CacheManager cacheManager = cachingProvider.getCacheManager();

        // Configure cache used by Bucket4j
        MutableConfiguration<String, Object> config = new MutableConfiguration<String, Object>()
                .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_MINUTE))
                .setStoreByValue(false)
                .setStatisticsEnabled(true);

        // Create cache named "buckets" as referenced in application.yml
        cacheManager.createCache("buckets", config);

        return new JCacheCacheResolver(cacheManager);
    }
}
