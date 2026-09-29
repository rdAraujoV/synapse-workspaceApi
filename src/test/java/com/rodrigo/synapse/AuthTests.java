package com.rodrigo.synapse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rodrigo.synapse.config.AuthRateLimitFilter;
import com.rodrigo.synapse.dto.AuthResponseDTO;
import com.rodrigo.synapse.service.AuthService;
import com.rodrigo.synapse.service.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.http.MediaType;


import com.rodrigo.synapse.entity.UserEntity;
import com.rodrigo.synapse.repository.UserRepository;

import java.util.Date;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class AuthTests {
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
    JwtService jwtService;

    @Autowired
    AuthRateLimitFilter authRateLimitFilter;

    @Autowired
    AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String validUserJson() {
        return """
                {
                    "email": "email@email.com",
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
    void deveRegistrarUsuario() throws Exception {
        registerValidUser();

        assertEquals(1, userRepository.count());

        UserEntity user = userRepository.findByEmail("email@email.com")
                .orElseThrow();
        assertEquals("email@email.com", user.getEmail());
    }

    @Test
    void naoDeveRegistrarEmailDuplicado() throws Exception {

        registerValidUser();

        assertEquals(1, userRepository.count());

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUserJson()))
                .andExpect(status().isConflict());

        assertEquals(1, userRepository.count());
    }

    @Test
    void naoDeveRegistrarEmailInvalido() throws Exception {
        String json = """
                {
                    "email": "email",
                    "password": "12345678Ii@#$"
                }
                """;

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isBadRequest());

        assertEquals(0, userRepository.count());
    }

    @Test
    void naoDeveRegistrarSenhaInvalida() throws Exception {
        String json = """
                {
                    "email": "email@email.com",
                    "password": "1234567"
                }
                """;

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isBadRequest());

        assertEquals(0, userRepository.count());
    }

    @Test
    void naoDeveRegistrarEmailVazio() throws Exception {
        String json = """
                {
                    "email": "",
                    "password": "12345678Ii@#$"
                }
                """;
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isBadRequest());
        assertEquals(0, userRepository.count());
    }

    @Test
    void naoDeveRegistrarSenhaVazia() throws Exception {
        String json = """
                {
                    "email": "email@email",
                    "password": ""
                }
                """;
        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isBadRequest());
        assertEquals(0, userRepository.count());
    }

    @Test
    void deveFazerLoginComCredenciaisValidas() throws Exception {
        registerValidUser();

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUserJson()))
                .andExpect(status().isOk());
    }

    @Test
    void naoDeveFazerLoginComSenhaIncorreta() throws Exception {

        registerValidUser();

        String login = """
                {
                    "email": "email@email.com",
                    "password": "12345678"
                }
                """;
        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(login))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void naoDeveFazerLoginComUsuarioInexistente() throws Exception {
        registerValidUser();

        String login = """
                {
                    "email": "wrong@email.com",
                    "password": "12345678Ii@#$"
                }
                """;
        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(login))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornarJwtValidoAoFazerLogin() throws Exception {
        registerValidUser();

        MvcResult result = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUserJson()))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        AuthResponseDTO responseDTO =
                objectMapper.readValue(response, AuthResponseDTO.class);

        assertNotNull(responseDTO.getToken());
        assertFalse(responseDTO.getToken().isBlank());
        assertTrue(jwtService.isTokenValid(responseDTO.getToken()));
    }

    @Test
    void deveAcessarEndPointProtegido() throws Exception {
        registerValidUser();

        String response = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validUserJson()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse().
                getContentAsString();

        ObjectMapper objectMapper = new ObjectMapper();

        String token = objectMapper
                .readTree(response)
                .get("token")
                .asText();

        mockMvc.perform(
                        get("/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                ))
                .andExpect(status().isOk())
                .andExpect(content().string("email@email.com"));
    }

    @Test
    void naoDeveAcessarEndPointProtegidoTokenInvalido() throws Exception {
        mockMvc.perform(
                        get("/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer invalid-token"
                                ))
                .andExpect(status().isForbidden());
    }

    @Test
    void naoDeveAcessarEndPointProtegidoSemToken() throws Exception {
        mockMvc.perform(
                        get("/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void naoDeveAcessarEndPointProtegidoTokenVazio() throws Exception {
        String token = "";
        mockMvc.perform(
                        get("/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                ))
                .andExpect(status().isForbidden());
    }

    @Test
    void naoDeveAcessarEndPointProtegidoComTokenExpirado() throws Exception {
        String expiredToken = jwtService.generateToken(
                "email@email.com",
                -60_000
        );

        mockMvc.perform(
                        get("/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + expiredToken
                                ))
                .andExpect(status().isForbidden());
    }
}