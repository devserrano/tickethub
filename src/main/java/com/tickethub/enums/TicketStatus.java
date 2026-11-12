package com.tickethub.enums;

/**
 * Los 8 estatus del ticket. El "pauseSla / stopSla" indica qué hace el reloj de SLA en cada uno.
 * Las transiciones válidas se validan en el service de tickets (cualquier otra se rechaza).
 */
public enum TicketStatus {
    PENDING_REVIEW,  // 1. En revisión: el reloj corre
    REJECTED,        // 2. Rechazado: el reloj se pausa, el USER corrige
    APPROVED,        // 3. Por asignar: el reloj corre
    IN_PROGRESS,     // 4. En proceso: el reloj corre
    ON_HOLD,         // 5. En espera de información: el reloj se pausa
    RESOLVED,        // 6. Resuelto: el reloj se detiene
    CLOSED,          // 7. Cerrado (final)
    CANCELLED;       // 8. Cancelado (final)

    public boolean isFinal() {
        return this == CLOSED || this == CANCELLED;
    }

    public boolean pausesSla() {
        return this == REJECTED || this == ON_HOLD;
    }

    public boolean stopsSla() {
        return this == RESOLVED || this == CLOSED || this == CANCELLED;
    }
}
