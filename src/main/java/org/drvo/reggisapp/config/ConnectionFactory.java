package org.drvo.reggisapp.config;

import java.sql.Connection;
import java.sql.SQLException;
import org.drvo.reggisapp.domain.service.TransactionBoundary;

public interface ConnectionFactory extends TransactionBoundary {
    <T> T withConnection(SqlWork<T> work);

    @FunctionalInterface
    interface SqlWork<T> {
        T apply(Connection connection) throws SQLException;
    }
}
