package com.rodrigo.synapse.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginDto {
    @NotBlank(message = "Email must be present")
    private String email;

    @NotBlank(message = "Password must be present")
    private String password; // no hash

    // getters e setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
