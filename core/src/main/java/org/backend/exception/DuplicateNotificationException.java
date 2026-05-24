package org.backend.exception;

public class DuplicateNotificationException extends RuntimeException{
    public DuplicateNotificationException(String idempotencyKey) {
        super("Duplicate notification: key=" + idempotencyKey);
    }
}
