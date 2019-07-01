package com.wire.bots.sdk.exceptions;

public class AuthException extends HttpException {
    public AuthException(String message, int code) {
        super(message, code);
    }

    public AuthException(int code) {
        super(code);
    }

    @Override
    public String toString() {
        return String.format("AuthException: %s, status: %s", getMessage(), getStatusCode());
    }
}