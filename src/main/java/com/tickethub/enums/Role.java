package com.tickethub.enums;

/** Todo registro nace USER; solo un ADMIN asigna los demás roles. */
public enum Role {
    USER,        // solicitante: solo ve sus tickets
    LIBERADOR,   // revisor nivel 1: ve estatus 1-3 y los que liberó
    AGENT,       // soporte: solo ve los asignados
    ADMIN        // control total
}
