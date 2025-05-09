package com.travel.management.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
