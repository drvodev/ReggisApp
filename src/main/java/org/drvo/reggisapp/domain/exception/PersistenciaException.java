package org.drvo.reggisapp.domain.exception;

public class PersistenciaException extends RuntimeException {
    public PersistenciaException(String message, Throwable cause) {
        super(message, cause);
    }
}
