package com.tickethub.enums;

/** Prioridad con sus horas de SLA (respuesta / resolución). Valores propuestos, pendientes de confirmar con el equipo. */
public enum Priority {
    LOW(24, 120),
    MEDIUM(8, 72),
    HIGH(4, 24),
    CRITICAL(1, 4);

    private final int responseHours;
    private final int resolutionHours;

    Priority(int responseHours, int resolutionHours) {
        this.responseHours = responseHours;
        this.resolutionHours = resolutionHours;
    }

    public int getResponseHours() {
        return responseHours;
    }

    public int getResolutionHours() {
        return resolutionHours;
    }
}
