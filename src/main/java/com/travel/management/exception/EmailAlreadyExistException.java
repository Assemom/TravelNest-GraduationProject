package com.travel.management.exception;

public class EmailAlreadyExistException extends RuntimeException {
    public EmailAlreadyExistException(){
        //printed on the console of my ide
        super("Email Is Already Taken");
    }
}
