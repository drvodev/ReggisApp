package org.drvo.reggisapp.repository.sqlite;

import org.drvo.reggisapp.config.ConnectionFactory;
import org.drvo.reggisapp.domain.model.Moneda;
import org.drvo.reggisapp.domain.model.Pago;
import org.drvo.reggisapp.repository.AbstractJdbcRepository;
import org.drvo.reggisapp.repository.PagoRepository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class SqlitePagoRepository extends AbstractJdbcRepository implements PagoRepository {
    public SqlitePagoRepository(ConnectionFactory conexiones) { super(conexiones); }

    @Override
    public Pago guardar(Pago pago) {
        return withConnection(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO pagos(pedido_id, fecha_pago, monto_bs, monto_original, moneda_original, tasa_cambio) " +
                            "VALUES(?, ?, ?, ?, ?, ?)")) {
                sentencia.setLong(1, pago.getPedidoId());
                sentencia.setString(2, pago.getFechaPago().toString());
                sentencia.setString(3, pago.getMontoBs().toPlainString());
                sentencia.setString(4, pago.getMontoOriginal().toPlainString());
                sentencia.setString(5, pago.getMonedaOriginal().name());
                sentencia.setString(6, pago.getTasaCambio().toPlainString());
                sentencia.executeUpdate();
            }
            return pago.conId(ultimoId(conexion));
        });
    }

    @Override
    public List<Pago> listarPorPedido(Long pedidoId) {
        return withConnection(conexion -> {
            List<Pago> pagos = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, pedido_id, fecha_pago, monto_bs, monto_original, moneda_original, tasa_cambio " +
                            "FROM pagos WHERE pedido_id = ? ORDER BY fecha_pago, id")) {
                sentencia.setLong(1, pedidoId);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) {
                        pagos.add(new Pago(resultado.getLong("id"), resultado.getLong("pedido_id"),
                                LocalDate.parse(resultado.getString("fecha_pago")),
                                new BigDecimal(resultado.getString("monto_bs")),
                                new BigDecimal(resultado.getString("monto_original")),
                                Moneda.valueOf(resultado.getString("moneda_original")),
                                new BigDecimal(resultado.getString("tasa_cambio"))));
                    }
                }
            }
            return List.copyOf(pagos);
        });
    }

    @Override
    public BigDecimal sumarPagosBs(Long pedidoId) {
        return listarPorPedido(pedidoId).stream().map(Pago::getMontoBs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
