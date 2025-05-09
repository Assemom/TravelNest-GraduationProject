package com.travel.management.exception;

public class EmailFailedToSendException extends RuntimeException {
    private static final long serialVersionUID = 1;
    public EmailFailedToSendException(String message){

        super("the email failed to send and this is from the exception class" + message);
    }
}
