package com.travel.management.exception;

public class IncorrectVerificationCodeException extends RuntimeException {
    private static final Long serialVersionUID = 1L;
    public IncorrectVerificationCodeException(){
        super("the code passed did not match the verification code that has been sent");
    }
}
