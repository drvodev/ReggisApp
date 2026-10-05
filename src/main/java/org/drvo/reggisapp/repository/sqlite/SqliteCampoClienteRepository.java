package org.drvo.reggisapp.repository.sqlite;

import org.drvo.reggisapp.config.ConnectionFactory;
import org.drvo.reggisapp.domain.model.CampoCliente;
import org.drvo.reggisapp.repository.AbstractJdbcRepository;
import org.drvo.reggisapp.repository.CampoClienteRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class SqliteCampoClienteRepository extends AbstractJdbcRepository implements CampoClienteRepository {
    public SqliteCampoClienteRepository(ConnectionFactory conexiones) { super(conexiones); }

    @Override
    public CampoCliente guardar(CampoCliente campo) {
        return withConnection(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO campos_cliente(clave, etiqueta, activo) VALUES(?, ?, ?)")) {
                sentencia.setString(1, campo.getClave());
                sentencia.setString(2, campo.getEtiqueta());
                sentencia.setInt(3, campo.isActivo() ? 1 : 0);
                sentencia.executeUpdate();
            }
            return new CampoCliente(ultimoId(conexion), campo.getClave(), campo.getEtiqueta(), campo.isActivo());
        });
    }

    @Override
    public List<CampoCliente> listarActivos() {
        return withConnection(conexion -> {
            List<CampoCliente> campos = new ArrayList<>();
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, clave, etiqueta, activo FROM campos_cliente WHERE activo = 1 ORDER BY etiqueta COLLATE NOCASE");
                 ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) campos.add(new CampoCliente(resultado.getLong("id"),
                        resultado.getString("clave"), resultado.getString("etiqueta"), true));
            }
            return List.copyOf(campos);
        });
    }
}
