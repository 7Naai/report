package com.pedidos360.report.service;

public class InvalidAuditEventException extends RuntimeException {

    public InvalidAuditEventException(String message) {
        super(message);
    }

    public InvalidAuditEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
