package com.cosw.councilOfSocialWork.exception;

public class TokenNotFoundException extends RuntimeException{

    public TokenNotFoundException(final String message){
        super(message);
    }
}
