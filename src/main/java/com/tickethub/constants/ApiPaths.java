package com.tickethub.constants;

/** Rutas base de la API. */
public final class ApiPaths {

    public static final String BASE = "/api/v1";
    public static final String AUTH = BASE + "/auth";
    public static final String AUTH_LOGIN = AUTH + "/login";
    public static final String AUTH_REGISTER = AUTH + "/register";
    public static final String AUTH_LOGOUT = AUTH + "/logout";

    private ApiPaths() {
    }
}
