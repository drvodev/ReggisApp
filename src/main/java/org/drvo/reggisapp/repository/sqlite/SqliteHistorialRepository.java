package org.drvo.reggisapp.repository.sqlite;

import org.drvo.reggisapp.config.ConnectionFactory;
import org.drvo.reggisapp.domain.model.RegistroHistorial;
import org.drvo.reggisapp.repository.AbstractJdbcRepository;
import org.drvo.reggisapp.repository.HistorialRepository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class SqliteHistorialRepository extends AbstractJdbcRepository implements HistorialRepository {
    public SqliteHistorialRepository(ConnectionFactory conexiones) { super(conexiones); }

    @Override
    public RegistroHistorial guardar(RegistroHistorial registro) {
        return withConnection(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO historial(tipo_entidad, entidad_id, tipo_evento, fecha_registro, motivo, monto_bs, resolucion) " +
                            "VALUES(?, ?, ?, ?, ?, ?, ?)")) {
                sentencia.setString(1, registro.getTipoEntidad());
                sentencia.setLong(2, registro.getEntidadId());
                sentencia.setString(3, registro.getTipoEvento());
                sentencia.setString(4, registro.getFechaRegistro().toString());
                sentencia.setString(5, registro.getMotivo());
                sentencia.setString(6, registro.getMontoBs() == null ? null : registro.getMontoBs().toPlainString());
                sentencia.setString(7, registro.getResolucion());
                sentencia.executeUpdate();
            }
            long id = ultimoId(conexion);
            return new RegistroHistorial(id, registro.getTipoEntidad(), registro.getEntidadId(),
                    registro.getTipoEvento(), registro.getFechaRegistro(), registro.getMotivo(),
                    registro.getMontoBs(), registro.getResolucion());
        });
    }

    @Override
    public List<RegistroHistorial> listarPorEntidad(String tipoEntidad, Long entidadId) {
        return withConnection(conexion -> {
            List<RegistroHistorial> registros = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, tipo_entidad, entidad_id, tipo_evento, fecha_registro, motivo, monto_bs, resolucion " +
                            "FROM historial WHERE tipo_entidad = ? AND entidad_id = ? ORDER BY fecha_registro DESC, id DESC")) {
                sentencia.setString(1, tipoEntidad);
                sentencia.setLong(2, entidadId);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    while (resultado.next()) {
                        String monto = resultado.getString("monto_bs");
                        registros.add(new RegistroHistorial(resultado.getLong("id"),
                                resultado.getString("tipo_entidad"), resultado.getLong("entidad_id"),
                                resultado.getString("tipo_evento"), LocalDateTime.parse(resultado.getString("fecha_registro")),
                                resultado.getString("motivo"), monto == null ? null : new BigDecimal(monto),
                                resultado.getString("resolucion")));
                    }
                }
            }
            return List.copyOf(registros);
        });
    }
}
