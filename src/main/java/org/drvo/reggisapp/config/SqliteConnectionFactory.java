package org.drvo.reggisapp.config;

import org.drvo.reggisapp.domain.exception.PersistenciaException;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.function.Supplier;

/** Abre conexiones SQLite y comparte una conexión durante cada operación transaccional. */
public final class SqliteConnectionFactory implements ConnectionFactory {
    private final String jdbcUrl;
    private final ThreadLocal<Connection> transaccionActual = new ThreadLocal<>();

    public SqliteConnectionFactory(Path archivoBaseDatos) {
        Objects.requireNonNull(archivoBaseDatos, "La ruta de la base de datos es obligatoria.");
        this.jdbcUrl = "jdbc:sqlite:" + archivoBaseDatos.toAbsolutePath();
    }

    @Override
    public <T> T withConnection(SqlWork<T> work) {
        Connection transaccion = transaccionActual.get();
        if (transaccion != null) {
            try {
                return work.apply(transaccion);
            } catch (SQLException excepcion) {
                throw new PersistenciaException("No se pudo ejecutar la operación SQLite.", excepcion);
            }
        }

        try (Connection conexion = abrirConexion()) {
            return work.apply(conexion);
        } catch (SQLException excepcion) {
            throw new PersistenciaException("No se pudo acceder a la base de datos local.", excepcion);
        }
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        if (transaccionActual.get() != null) return work.get();
        return withConnection(conexion -> {
            boolean autoCommitOriginal = conexion.getAutoCommit();
            conexion.setAutoCommit(false);
            transaccionActual.set(conexion);
            try {
                T resultado = work.get();
                conexion.commit();
                return resultado;
            } catch (RuntimeException | Error excepcion) {
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    excepcion.addSuppressed(errorRollback);
                }
                throw excepcion;
            } finally {
                transaccionActual.remove();
                conexion.setAutoCommit(autoCommitOriginal);
            }
        });
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    private Connection abrirConexion() throws SQLException {
        Connection conexion = DriverManager.getConnection(jdbcUrl);
        try (Statement sentencia = conexion.createStatement()) {
            sentencia.execute("PRAGMA foreign_keys = ON");
            sentencia.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException excepcion) {
            conexion.close();
            throw excepcion;
        }
        return conexion;
    }
}
