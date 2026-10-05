package com.rodrigo.synapse;

import com.rodrigo.synapse.config.AuthRateLimitFilter;
import com.rodrigo.synapse.repository.UserRepository;
import com.rodrigo.synapse.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@ActiveProfiles("ratelimittest")
@AutoConfigureMockMvc
public class RateLimitTests {
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl);

        registry.add(
                "spring.datasource.username",
                postgres::getUsername);

        registry.add(
                "spring.datasource.password",
                postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthService authService;

    @Autowired
    AuthRateLimitFilter authRateLimitFilter;

    private String validUserJson() {
        return """
                {
                    "email": "email@email.com",
                    "password": "12345678Ii@#$"
                }
                """;
    }

    private String userEmail1() {
        return """
                {
                    "email": "email1@email.com",
                    "password": "12345678Ii@#$"
                }
                """;
    }

    private String userEmail2() {
        return """
                {
                    "email": "email2@email.com",
                    "password": "12345678Ii@#$"
                }
                """;
    }

    private void registerValidUser() throws Exception {
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUserJson()))
                .andExpect(status().isCreated());
    }

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
        authRateLimitFilter.clearCache();
        authService.clearCache();
    }

    @Test
    void deveAplicarRateLimitPorIpLogin() throws Exception {
        registerValidUser();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(
                            post("/auth/login")
                                    .with(request -> {
                                        request.setRemoteAddr("192.168.1.10");
                                        return request;
                                    })
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(userEmail1()))
                    .andExpect(status().isUnauthorized());
        }
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(
                            post("/auth/login")
                                    .with(request -> {
                                        request.setRemoteAddr("192.168.1.10");
                                        return request;
                                    })
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(userEmail2()))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(
                        post("/auth/login")
                                .with(request -> {
                                    request.setRemoteAddr("192.168.1.10");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUserJson()))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(
                        post("/auth/login")
                                .with(request -> {
                                    request.setRemoteAddr("192.168.1.20");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUserJson()))
                .andExpect(status().isOk());
    }

    @Test
    void deveAplicarRateLimitPorIpRegister() throws Exception {
        mockMvc.perform(
                        post("/auth/register")
                                .with(request -> {
                                    request.setRemoteAddr("192.168.1.10");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUserJson()))
                .andExpect(status().isCreated());

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(
                            post("/auth/register")
                                    .with(request -> {
                                        request.setRemoteAddr("192.168.1.10");
                                        return request;
                                    })
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(validUserJson()))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(
                        post("/auth/register")
                                .with(request -> {
                                    request.setRemoteAddr("192.168.1.10");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUserJson()))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(
                        post("/auth/register")
                                .with(request -> {
                                    request.setRemoteAddr("192.168.1.20");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUserJson()))
                .andExpect(status().isCreated());
    }

    @Test
    void naoDeveBurlarRateLimitComXForwardedFor() throws Exception {
        String body = """
                {"email": "user%d@email.com", "password": "12345678Ii@#$"}
                """;
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                            .with(r -> {
                                r.setRemoteAddr("192.168.1.10");
                                return r;
                            })
                            .header("X-Forwarded-For", "10.0.0." + i)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body.formatted(i)))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/auth/login")
                        .with(r -> {
                            r.setRemoteAddr("192.168.1.10");
                            return r;
                        })
                        .header("X-Forwarded-For", "10.0.0.99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(99)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void deveAplicarRateLimitPorEmailLogin() throws Exception {
        registerValidUser();
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(validUserJson()))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(validUserJson()))
                .andExpect(status().isTooManyRequests());
    }
}
