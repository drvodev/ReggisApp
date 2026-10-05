package org.drvo.reggisapp.repository.sqlite;

import org.drvo.reggisapp.config.DatabaseConfig;
import org.drvo.reggisapp.config.SqliteConnectionFactory;
import org.drvo.reggisapp.domain.model.Cliente;
import org.drvo.reggisapp.domain.model.Moneda;
import org.drvo.reggisapp.domain.model.RegistroHistorial;
import org.drvo.reggisapp.domain.exception.PersistenciaException;
import org.drvo.reggisapp.domain.service.CobranzaServiceImpl;
import org.drvo.reggisapp.domain.service.TasaCambioManualService;
import org.drvo.reggisapp.repository.sqlite.SqliteCampoClienteRepository;
import org.drvo.reggisapp.repository.sqlite.SqliteClienteRepository;
import org.drvo.reggisapp.repository.sqlite.SqliteHistorialRepository;
import org.drvo.reggisapp.repository.sqlite.SqliteMovimientoDeudaRepository;
import org.drvo.reggisapp.repository.sqlite.SqlitePagoRepository;
import org.drvo.reggisapp.repository.sqlite.SqlitePedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqliteSecurityTest {
    @TempDir Path directorioTemporal;
    private SqliteConnectionFactory conexiones;
    private SqliteClienteRepository clientes;
    private SqlitePedidoRepository pedidos;
    private SqliteHistorialRepository historial;

    @BeforeEach
    void prepararBaseTemporal() {
        DatabaseConfig config = new DatabaseConfig(directorioTemporal.resolve("seguridad.db"));
        conexiones = config.crearConnectionFactory();
        config.inicializar(conexiones);
        clientes = new SqliteClienteRepository(conexiones);
        pedidos = new SqlitePedidoRepository(conexiones);
        historial = new SqliteHistorialRepository(conexiones);
    }

    @Test
    void payloadEnCamposDeClienteSeGuardaComoTextoSinEjecutarse() {
        String payload = "Robert'); DROP TABLE clientes; --";
        Cliente original = new Cliente(null, payload, org.drvo.reggisapp.domain.model.EstadoCliente.ACTIVO,
                Map.of("rif'); DROP TABLE clientes; --", "x' OR 1=1 --"));

        Cliente guardado = clientes.guardar(original);

        assertEquals(payload, clientes.buscarPorId(guardado.getId()).orElseThrow().getNombre());
        assertEquals("x' OR 1=1 --", clientes.buscarPorId(guardado.getId()).orElseThrow()
                .getDatosAdicionales().get("rif'); DROP TABLE clientes; --"));
        assertEquals(1, clientes.listarTodos().size());
        assertEquals(1, clientes.listarActivos().size());
    }

    @Test
    void payloadEnDescripcionDePedidoSeGuardaComoTextoSinEjecutarse() {
        Cliente cliente = clientes.guardar(Cliente.nuevo("Cliente de prueba"));
        CobranzaServiceImpl cobranza = new CobranzaServiceImpl(clientes, pedidos,
                new SqlitePagoRepository(conexiones), historial,
                new SqliteMovimientoDeudaRepository(conexiones), new TasaCambioManualService(),
                Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC), conexiones);
        String payload = "Pedido'); DELETE FROM pedidos; --";

        var pedido = cobranza.crearPedido(cliente.getId(), payload, new BigDecimal("123.45"), Moneda.BS,
                new BigDecimal("800.40"));

        assertEquals(payload, pedidos.buscarPorId(pedido.getId()).orElseThrow().getDescripcion());
        assertEquals(1, pedidos.listarPorCliente(cliente.getId()).size());
        assertEquals("PEDIDO_CREADO", historial.listarPorEntidad("PEDIDO", pedido.getId()).getFirst().getTipoEvento());
    }

    @Test
    void rollbackReviertePedidoYEventosCuandoUnaOperacionFalla() {
        Cliente cliente = clientes.guardar(Cliente.nuevo("Cliente rollback"));

        assertThrows(IllegalStateException.class, () -> conexiones.inTransaction(() -> {
            pedidos.guardar(org.drvo.reggisapp.domain.model.Pedido.nuevo(cliente.getId(), "Pedido temporal",
                    new BigDecimal("10.00"), new BigDecimal("10.00"), Moneda.BS,
                    new BigDecimal("1.00"), java.time.LocalDate.of(2026, 10, 4)));
            throw new IllegalStateException("forzar rollback");
        }));

        assertFalse(pedidos.listarPorCliente(cliente.getId()).size() > 0);
    }

    @Test
    void historialDeAuditoriaNoPuedeEditarNiEliminarRegistros() {
        historial.guardar(new RegistroHistorial(null, "PEDIDO", 42L, "PEDIDO_ANULADO",
                LocalDateTime.parse("2026-10-04T12:00:00"), "Cambio solicitado por cliente",
                new BigDecimal("50.00"), "CONDONAR_SALDO"));

        assertThrows(PersistenciaException.class, () -> conexiones.withConnection(conexion -> {
            try (var sentencia = conexion.createStatement()) {
                sentencia.executeUpdate("UPDATE historial SET motivo = 'alterado' WHERE entidad_id = 42");
            }
            return null;
        }));
        assertThrows(PersistenciaException.class, () -> conexiones.withConnection(conexion -> {
            try (var sentencia = conexion.createStatement()) {
                sentencia.executeUpdate("DELETE FROM historial WHERE entidad_id = 42");
            }
            return null;
        }));

        assertEquals("Cambio solicitado por cliente",
                historial.listarPorEntidad("PEDIDO", 42L).getFirst().getMotivo());
    }
}
