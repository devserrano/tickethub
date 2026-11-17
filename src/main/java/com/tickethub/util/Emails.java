package com.tickethub.util;

/** Utilidades de correo. */
public final class Emails {

    private Emails() {
    }

    /** Los correos se guardan y se buscan siempre sin espacios y en minúsculas. */
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
