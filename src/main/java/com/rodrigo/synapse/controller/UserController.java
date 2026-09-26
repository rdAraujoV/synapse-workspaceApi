package com.rodrigo.synapse.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/users/me")
    public String me(Authentication authentication) {
        return authentication.getName();
    }
}