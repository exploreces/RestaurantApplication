package com.epam.edp.demo.exception;


public class TooLateForCancellationException extends RuntimeException {
    public TooLateForCancellationException(String message){
        super(message);
    }
}
