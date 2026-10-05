package com.rodrigo.synapse.config;

import java.io.IOException;
import java.time.Duration;

import com.rodrigo.synapse.exception.TooManyRequestsException;
import io.github.bucket4j.Bucket;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    @Value("${rate-limit.capacity}")
    private int capacity;
    @Value("${rate-limit.refill-rate}")
    private int refillRate;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    private final Cache<String, Bucket> ipBuckets = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public void clearCache() {
        ipBuckets.invalidateAll();
    }

    private Bucket newIpBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(refillRate, Duration.ofMinutes(1)))
                .build();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        Bucket ipBucket = ipBuckets.get(ip, k -> newIpBucket());

        if (!ipBucket.tryConsume(1)) {
            resolver.resolveException(request, response, null, new TooManyRequestsException("Too many requests"));
            return;
        }

        filterChain.doFilter(request, response);
    }
}
