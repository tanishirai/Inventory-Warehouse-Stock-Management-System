package com.stockpilot.exception;

/** Thrown when login credentials are invalid or a session-less user attempts a protected action. */
public class AuthenticationException extends Exception {
    public AuthenticationException(String message) {
        super(message);
    }
}
