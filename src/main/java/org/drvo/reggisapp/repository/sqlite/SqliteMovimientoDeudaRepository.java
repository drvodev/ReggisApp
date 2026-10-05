package org.drvo.reggisapp.repository.sqlite;

import org.drvo.reggisapp.config.ConnectionFactory;
import org.drvo.reggisapp.domain.model.MovimientoDeuda;
import org.drvo.reggisapp.repository.AbstractJdbcRepository;
import org.drvo.reggisapp.repository.MovimientoDeudaRepository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class SqliteMovimientoDeudaRepository extends AbstractJdbcRepository implements MovimientoDeudaRepository {
    public SqliteMovimientoDeudaRepository(ConnectionFactory conexiones) { super(conexiones); }

    @Override
    public MovimientoDeuda guardar(MovimientoDeuda movimiento) {
        return withConnection(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO movimientos_deuda(pedido_origen_id, pedido_destino_id, monto_bs, fecha, motivo) " +
                            "VALUES(?, ?, ?, ?, ?)")) {
                sentencia.setLong(1, movimiento.getPedidoOrigenId());
                sentencia.setLong(2, movimiento.getPedidoDestinoId());
                sentencia.setString(3, movimiento.getMontoBs().toPlainString());
                sentencia.setString(4, movimiento.getFecha().toString());
                sentencia.setString(5, movimiento.getMotivo());
                sentencia.executeUpdate();
            }
            return new MovimientoDeuda(ultimoId(conexion), movimiento.getPedidoOrigenId(),
                    movimiento.getPedidoDestinoId(), movimiento.getMontoBs(), movimiento.getFecha(), movimiento.getMotivo());
        });
    }

    @Override
    public BigDecimal sumarEntradasBs(Long pedidoId) {
        return sumar("pedido_destino_id", pedidoId);
    }

    @Override
    public BigDecimal sumarSalidasBs(Long pedidoId) {
        return sumar("pedido_origen_id", pedidoId);
    }

    private BigDecimal sumar(String columna, Long pedidoId) {
        // El nombre de columna solo procede de los dos literales internos anteriores.
        return withConnection(conexion -> {
            BigDecimal total = BigDecimal.ZERO;
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT monto_bs FROM movimientos_deuda WHERE " + columna + " = ?")) {
                sentencia.setLong(1, pedidoId);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) total = total.add(new BigDecimal(resultado.getString(1)));
                }
            }
            return total;
        });
    }
}
