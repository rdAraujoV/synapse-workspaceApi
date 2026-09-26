package com.rodrigo.synapse.controller;

import com.rodrigo.synapse.dto.AuthResponseDTO;
import com.rodrigo.synapse.dto.LoginDto;
import com.rodrigo.synapse.dto.RegisterDTO;
import com.rodrigo.synapse.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/auth"})
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterDTO request){
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginDto request){
        String token = authService.login(request);
        return ResponseEntity.ok(new AuthResponseDTO(token));
    }
}
