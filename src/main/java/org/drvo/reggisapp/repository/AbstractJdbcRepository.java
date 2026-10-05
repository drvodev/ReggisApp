package org.drvo.reggisapp.repository;

import org.drvo.reggisapp.config.ConnectionFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class AbstractJdbcRepository {
    private final ConnectionFactory conexiones;

    protected AbstractJdbcRepository(ConnectionFactory conexiones) {
        this.conexiones = Objects.requireNonNull(conexiones);
    }

    protected <T> T withConnection(ConnectionFactory.SqlWork<T> work) {
        return conexiones.withConnection(work);
    }

    protected <T> T inTransaction(Supplier<T> work) {
        return conexiones.inTransaction(work);
    }

    protected long ultimoId(Connection conexion) throws SQLException {
        try (Statement sentencia = conexion.createStatement();
             ResultSet resultado = sentencia.executeQuery("SELECT last_insert_rowid()")) {
            if (!resultado.next()) throw new SQLException("No se pudo obtener el identificador generado.");
            return resultado.getLong(1);
        }
    }
}
