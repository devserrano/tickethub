package com.tickethub.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Ya existe una cuenta con el correo " + email);
    }
}
