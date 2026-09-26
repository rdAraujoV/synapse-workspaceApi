package com.rodrigo.synapse.exception;

import java.time.LocalDateTime;

public class ErrorResponseDTO {
    private int status;
    private String message;
    private LocalDateTime timestamp;

    public ErrorResponseDTO(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public int getStatus() {
        return status;
    }
    public String getMessage() {
        return message;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}