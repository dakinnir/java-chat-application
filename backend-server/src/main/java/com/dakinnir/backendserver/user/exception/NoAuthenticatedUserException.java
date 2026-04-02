package com.dakinnir.backendserver.user.exception;

public class NoAuthenticatedUserException extends RuntimeException {
    public NoAuthenticatedUserException(String message) {
        super(message);
    }
}
