package com.travel.management.exception;

public class PasswordReusedException extends RuntimeException {
    public PasswordReusedException(String message) {
        super(message);
    }
}
