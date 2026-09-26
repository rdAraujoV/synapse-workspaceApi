package com.rodrigo.synapse.dto;

public class AuthResponseDTO {
    private String token;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String token) {
        this.token = token;
    }

    // getters e setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}