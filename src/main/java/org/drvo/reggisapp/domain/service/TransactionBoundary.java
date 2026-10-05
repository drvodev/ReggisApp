package org.drvo.reggisapp.domain.service;

import java.util.function.Supplier;

/** Define una frontera transaccional sin acoplar el dominio a JDBC. */
public interface TransactionBoundary {
    <T> T inTransaction(Supplier<T> action);

    static TransactionBoundary directa() {
        return new TransactionBoundary() {
            @Override
            public <T> T inTransaction(Supplier<T> action) {
                return action.get();
            }
        };
    }
}
