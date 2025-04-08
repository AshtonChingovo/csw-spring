package com.cosw.councilOfSocialWork.exception;

public class ResourceNotFoundException extends RuntimeException{

    public ResourceNotFoundException(final String message){
        super(message);
    }

}
