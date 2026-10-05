package com.rodrigo.synapse.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rodrigo.synapse.entity.UserEntity;
import com.rodrigo.synapse.exception.InvalidCredentialsException;
import com.rodrigo.synapse.exception.TooManyRequestsException;
import com.rodrigo.synapse.dto.RegisterDTO;
import com.rodrigo.synapse.dto.LoginDto;
import com.rodrigo.synapse.repository.UserRepository;

import io.github.bucket4j.Bucket;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// will populate the user entity with the hashed password, and validate the login credentials
@Service
public class AuthService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String dummyHash;

    @PostConstruct
    void initDummyHash() {
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Autowired
    UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Value("${rate-limit.capacity}")
    private int capacity;
    @Value("${rate-limit.refill-rate}")
    private int refillRate;

    public void register(RegisterDTO request) {
        UserEntity user = new UserEntity();
        // Exception for duplicated email
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return;
        }
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private final Cache<String, Bucket> loginBucketsByEmail = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public void clearCache() {
        loginBucketsByEmail.invalidateAll();
    }

    private Bucket bucketFor(String email) {
        return loginBucketsByEmail.get(email, k -> Bucket.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(refillRate, Duration.ofMinutes(1)))
                .build());
    }

    public String login(LoginDto request) {
        if (!bucketFor(request.getEmail()).tryConsume(1)) {
            throw new TooManyRequestsException("Too many requests");
        }
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        String passwordHash = user != null
                ? user.getPasswordHash()
                : dummyHash;

        if (!passwordEncoder.matches(request.getPassword(), passwordHash)) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        return jwtService.generateToken(user.getId());
    }
}