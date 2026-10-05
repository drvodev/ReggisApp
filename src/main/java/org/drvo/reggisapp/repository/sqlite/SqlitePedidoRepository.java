package org.drvo.reggisapp.repository.sqlite;

import org.drvo.reggisapp.config.ConnectionFactory;
import org.drvo.reggisapp.domain.exception.PersistenciaException;
import org.drvo.reggisapp.domain.model.EstadoPedido;
import org.drvo.reggisapp.domain.model.Moneda;
import org.drvo.reggisapp.domain.model.Pedido;
import org.drvo.reggisapp.repository.AbstractJdbcRepository;
import org.drvo.reggisapp.repository.PedidoRepository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SqlitePedidoRepository extends AbstractJdbcRepository implements PedidoRepository {
    public SqlitePedidoRepository(ConnectionFactory conexiones) { super(conexiones); }

    @Override
    public Pedido guardar(Pedido pedido) {
        return withConnection(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO pedidos(cliente_id, descripcion, monto_pedido_bs, monto_original, moneda_original, tasa_cambio, fecha_creacion, estado) " +
                            "VALUES(?, ?, ?, ?, ?, ?, ?, ?)")) {
                sentencia.setLong(1, pedido.getClienteId());
                sentencia.setString(2, pedido.getDescripcion());
                sentencia.setString(3, pedido.getMontoPedidoBs().toPlainString());
                sentencia.setString(4, pedido.getMontoOriginal().toPlainString());
                sentencia.setString(5, pedido.getMonedaOriginal().name());
                sentencia.setString(6, pedido.getTasaCambio().toPlainString());
                sentencia.setString(7, pedido.getFechaCreacion().toString());
                sentencia.setString(8, pedido.getEstado().name());
                sentencia.executeUpdate();
            }
            return pedido.conId(ultimoId(conexion));
        });
    }

    @Override
    public Optional<Pedido> buscarPorId(Long id) {
        return withConnection(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, cliente_id, descripcion, monto_pedido_bs, monto_original, moneda_original, tasa_cambio, fecha_creacion, estado " +
                            "FROM pedidos WHERE id = ?")) {
                sentencia.setLong(1, id);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    return resultado.next() ? Optional.of(mapear(resultado)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Pedido actualizar(Pedido pedido) {
        if (pedido.getId() == null) throw new PersistenciaException("El pedido debe tener identificador para actualizarse.", null);
        return withConnection(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "UPDATE pedidos SET estado = ? WHERE id = ?")) {
                sentencia.setString(1, pedido.getEstado().name());
                sentencia.setLong(2, pedido.getId());
                if (sentencia.executeUpdate() != 1) throw new PersistenciaException("No se encontró el pedido para actualizar.", null);
            }
            return pedido;
        });
    }

    @Override
    public List<Pedido> listarPorCliente(Long clienteId) {
        return withConnection(conexion -> {
            List<Pedido> pedidos = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, cliente_id, descripcion, monto_pedido_bs, monto_original, moneda_original, tasa_cambio, fecha_creacion, estado " +
                            "FROM pedidos WHERE cliente_id = ? ORDER BY id DESC")) {
                sentencia.setLong(1, clienteId);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) pedidos.add(mapear(resultado));
                }
            }
            return List.copyOf(pedidos);
        });
    }

    private Pedido mapear(ResultSet resultado) throws java.sql.SQLException {
        return new Pedido(resultado.getLong("id"), resultado.getLong("cliente_id"), resultado.getString("descripcion"),
                new BigDecimal(resultado.getString("monto_pedido_bs")),
                new BigDecimal(resultado.getString("monto_original")),
                Moneda.valueOf(resultado.getString("moneda_original")),
                new BigDecimal(resultado.getString("tasa_cambio")),
                LocalDate.parse(resultado.getString("fecha_creacion")),
                EstadoPedido.valueOf(resultado.getString("estado")));
    }
}
