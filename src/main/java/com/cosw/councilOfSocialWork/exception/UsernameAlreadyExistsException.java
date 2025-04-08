package com.cosw.councilOfSocialWork.exception;

public class UsernameAlreadyExistsException extends RuntimeException{

    public UsernameAlreadyExistsException(final String message){
        super(message);
    }
}
