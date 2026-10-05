package org.drvo.reggisapp.config;

import org.drvo.reggisapp.domain.exception.PersistenciaException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

/** Ubica e inicializa el archivo local SQLite de la aplicación. */
public final class DatabaseConfig {
    private static final int VERSION_ESQUEMA = 2;
    private final Path archivoBaseDatos;

    public DatabaseConfig() {
        this(rutaLocalAplicacion().resolve("reggisapp.db"));
    }

    public DatabaseConfig(Path archivoBaseDatos) {
        this.archivoBaseDatos = Objects.requireNonNull(archivoBaseDatos).toAbsolutePath();
    }

    public Path getArchivoBaseDatos() {
        return archivoBaseDatos;
    }

    public SqliteConnectionFactory crearConnectionFactory() {
        return new SqliteConnectionFactory(archivoBaseDatos);
    }

    public void inicializar(SqliteConnectionFactory conexiones) {
        try {
            Files.createDirectories(archivoBaseDatos.getParent());
            conexiones.withConnection(conexion -> {
                try (Statement sentencia = conexion.createStatement()) {
                    sentencia.execute("PRAGMA journal_mode = WAL");
                    int versionActual;
                    try (ResultSet resultado = sentencia.executeQuery("PRAGMA user_version")) {
                        versionActual = resultado.next() ? resultado.getInt(1) : 0;
                    }
                    if (versionActual > VERSION_ESQUEMA) {
                        throw new PersistenciaException("La base de datos es de una versión posterior a esta aplicación.", null);
                    }
                    if (versionActual == 0) {
                        crearEsquemaV1(sentencia);
                        versionActual = 1;
                    }
                    if (versionActual < 2) {
                        migrarV2(sentencia);
                        versionActual = 2;
                    }
                    sentencia.execute("PRAGMA user_version = " + VERSION_ESQUEMA);
                }
                return null;
            });
        } catch (PersistenciaException excepcion) {
            throw excepcion;
        } catch (Exception excepcion) {
            throw new PersistenciaException("No se pudo inicializar la base de datos local.", excepcion);
        }
    }

    private void crearEsquemaV1(Statement sentencia) throws java.sql.SQLException {
        List<String> ddl = List.of(
                "CREATE TABLE clientes (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL CHECK(length(trim(nombre)) > 0), " +
                        "estado TEXT NOT NULL CHECK(estado IN ('ACTIVO','INACTIVO')))" ,
                "CREATE TABLE campos_cliente (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, clave TEXT NOT NULL UNIQUE, etiqueta TEXT NOT NULL, " +
                        "activo INTEGER NOT NULL CHECK(activo IN (0,1)))",
                "CREATE TABLE cliente_datos_adicionales (" +
                        "cliente_id INTEGER NOT NULL REFERENCES clientes(id) ON DELETE RESTRICT, " +
                        "clave TEXT NOT NULL, valor TEXT NOT NULL, PRIMARY KEY(cliente_id, clave))",
                "CREATE TABLE pedidos (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "cliente_id INTEGER NOT NULL REFERENCES clientes(id) ON DELETE RESTRICT, " +
                        "descripcion TEXT NOT NULL CHECK(length(trim(descripcion)) > 0), " +
                        "monto_pedido_bs TEXT NOT NULL, monto_original TEXT NOT NULL, moneda_original TEXT NOT NULL " +
                        "CHECK(moneda_original IN ('BS','USD')), tasa_cambio TEXT NOT NULL, fecha_creacion TEXT NOT NULL, " +
                        "estado TEXT NOT NULL CHECK(estado IN ('ACTIVO','COMPLETADO','ANULADO')))" ,
                "CREATE TABLE pagos (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, pedido_id INTEGER NOT NULL REFERENCES pedidos(id) ON DELETE RESTRICT, " +
                        "fecha_pago TEXT NOT NULL, monto_bs TEXT NOT NULL, monto_original TEXT NOT NULL, " +
                        "moneda_original TEXT NOT NULL CHECK(moneda_original IN ('BS','USD')), tasa_cambio TEXT NOT NULL)",
                "CREATE TABLE movimientos_deuda (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "pedido_origen_id INTEGER NOT NULL REFERENCES pedidos(id) ON DELETE RESTRICT, " +
                        "pedido_destino_id INTEGER NOT NULL REFERENCES pedidos(id) ON DELETE RESTRICT, " +
                        "monto_bs TEXT NOT NULL, fecha TEXT NOT NULL, motivo TEXT NOT NULL)",
                "CREATE TABLE historial (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, tipo_entidad TEXT NOT NULL, entidad_id INTEGER NOT NULL, " +
                        "tipo_evento TEXT NOT NULL, fecha_registro TEXT NOT NULL, motivo TEXT, monto_bs TEXT, resolucion TEXT)",
                "CREATE INDEX idx_pedidos_cliente_estado ON pedidos(cliente_id, estado)",
                "CREATE INDEX idx_pagos_pedido_fecha ON pagos(pedido_id, fecha_pago)",
                "CREATE INDEX idx_movimientos_origen ON movimientos_deuda(pedido_origen_id)",
                "CREATE INDEX idx_movimientos_destino ON movimientos_deuda(pedido_destino_id)",
                "CREATE INDEX idx_historial_entidad_fecha ON historial(tipo_entidad, entidad_id, fecha_registro)");
        for (String instruccion : ddl) sentencia.execute(instruccion);
    }

    private void migrarV2(Statement sentencia) throws java.sql.SQLException {
        sentencia.execute("CREATE TRIGGER historial_inmutable_update BEFORE UPDATE ON historial " +
                "BEGIN SELECT RAISE(ABORT, 'El historial es inmutable'); END");
        sentencia.execute("CREATE TRIGGER historial_inmutable_delete BEFORE DELETE ON historial " +
                "BEGIN SELECT RAISE(ABORT, 'El historial es inmutable'); END");
    }

    private static Path rutaLocalAplicacion() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) return Path.of(localAppData, "ReggisApp");
        return Path.of(System.getProperty("user.home"), ".reggisapp");
    }
}
