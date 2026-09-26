package com.rodrigo.synapse.exception;

public class TooManyRequestsException extends RuntimeException{
    public TooManyRequestsException (String message){
        super(message);
    }
}