package com.tickethub.exception;

public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException() {
        super("El correo ya está registrado");
    }
}
