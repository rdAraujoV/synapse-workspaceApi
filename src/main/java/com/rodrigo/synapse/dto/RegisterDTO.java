package com.rodrigo.synapse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterDTO {
    @NotBlank(message = "Email must be present")
    @Email (message = "Invalid email")
    private String email;

    @NotBlank(message = "Password must be present")
    @Size(min = 8, message = "Password must be at least 8 characters")
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