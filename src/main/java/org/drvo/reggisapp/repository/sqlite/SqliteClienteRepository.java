package org.drvo.reggisapp.repository.sqlite;

import org.drvo.reggisapp.config.ConnectionFactory;
import org.drvo.reggisapp.domain.exception.PersistenciaException;
import org.drvo.reggisapp.domain.model.Cliente;
import org.drvo.reggisapp.domain.model.EstadoCliente;
import org.drvo.reggisapp.repository.AbstractJdbcRepository;
import org.drvo.reggisapp.repository.ClienteRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SqliteClienteRepository extends AbstractJdbcRepository implements ClienteRepository {
    public SqliteClienteRepository(ConnectionFactory conexiones) {
        super(conexiones);
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        return inTransaction(() -> withConnection(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "INSERT INTO clientes(nombre, estado) VALUES(?, ?)")) {
                sentencia.setString(1, cliente.getNombre());
                sentencia.setString(2, cliente.getEstado().name());
                sentencia.executeUpdate();
            }
            long id = ultimoId(conexion);
            guardarDatosAdicionales(conexion, id, cliente.getDatosAdicionales());
            return cliente.conId(id);
        }));
    }

    @Override
    public Optional<Cliente> buscarPorId(Long id) {
        return withConnection(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "SELECT id, nombre, estado FROM clientes WHERE id = ?")) {
                sentencia.setLong(1, id);
                try (ResultSet resultado = sentencia.executeQuery()) {
                    if (!resultado.next()) return Optional.empty();
                    return Optional.of(new Cliente(resultado.getLong("id"), resultado.getString("nombre"),
                            EstadoCliente.valueOf(resultado.getString("estado")),
                            leerDatosAdicionales(conexion, resultado.getLong("id"))));
                }
            }
        });
    }

    @Override
    public Cliente actualizar(Cliente cliente) {
        if (cliente.getId() == null) throw new PersistenciaException("El cliente debe tener identificador para actualizarse.", null);
        return inTransaction(() -> withConnection(conexion -> {
            try (PreparedStatement sentencia = conexion.prepareStatement(
                    "UPDATE clientes SET nombre = ?, estado = ? WHERE id = ?")) {
                sentencia.setString(1, cliente.getNombre());
                sentencia.setString(2, cliente.getEstado().name());
                sentencia.setLong(3, cliente.getId());
                if (sentencia.executeUpdate() != 1) throw new PersistenciaException("No se encontró el cliente para actualizar.", null);
            }
            try (PreparedStatement eliminar = conexion.prepareStatement(
                    "DELETE FROM cliente_datos_adicionales WHERE cliente_id = ?")) {
                eliminar.setLong(1, cliente.getId());
                eliminar.executeUpdate();
            }
            guardarDatosAdicionales(conexion, cliente.getId(), cliente.getDatosAdicionales());
            return cliente;
        }));
    }

    @Override
    public List<Cliente> listarActivos() {
        return listar(true);
    }

    @Override
    public List<Cliente> listarTodos() {
        return listar(false);
    }

    private List<Cliente> listar(boolean soloActivos) {
        return withConnection(conexion -> {
            List<Cliente> clientes = new ArrayList<>();
            Map<Long, Map<String, String>> datosPorCliente = new LinkedHashMap<>();
            try (PreparedStatement datos = conexion.prepareStatement(
                    "SELECT cliente_id, clave, valor FROM cliente_datos_adicionales ORDER BY cliente_id, clave");
                 ResultSet resultadoDatos = datos.executeQuery()) {
                while (resultadoDatos.next()) datosPorCliente
                        .computeIfAbsent(resultadoDatos.getLong("cliente_id"), ignorado -> new LinkedHashMap<>())
                        .put(resultadoDatos.getString("clave"), resultadoDatos.getString("valor"));
            }
            String sql = soloActivos
                    ? "SELECT id, nombre, estado FROM clientes WHERE estado = 'ACTIVO' ORDER BY nombre COLLATE NOCASE, id"
                    : "SELECT id, nombre, estado FROM clientes ORDER BY nombre COLLATE NOCASE, id";
            try (PreparedStatement sentencia = conexion.prepareStatement(sql);
                 ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) {
                    long id = resultado.getLong("id");
                    clientes.add(new Cliente(id, resultado.getString("nombre"),
                            EstadoCliente.valueOf(resultado.getString("estado")), datosPorCliente.get(id)));
                }
            }
            return List.copyOf(clientes);
        });
    }

    private Map<String, String> leerDatosAdicionales(java.sql.Connection conexion, long clienteId)
            throws java.sql.SQLException {
        Map<String, String> datos = new LinkedHashMap<>();
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "SELECT clave, valor FROM cliente_datos_adicionales WHERE cliente_id = ? ORDER BY clave")) {
            sentencia.setLong(1, clienteId);
            try (ResultSet resultado = sentencia.executeQuery()) {
                while (resultado.next()) datos.put(resultado.getString("clave"), resultado.getString("valor"));
            }
        }
        return datos;
    }

    private void guardarDatosAdicionales(java.sql.Connection conexion, long clienteId, Map<String, String> datos)
            throws java.sql.SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(
                "INSERT INTO cliente_datos_adicionales(cliente_id, clave, valor) VALUES(?, ?, ?)")) {
            for (Map.Entry<String, String> dato : datos.entrySet()) {
                sentencia.setLong(1, clienteId);
                sentencia.setString(2, dato.getKey());
                sentencia.setString(3, dato.getValue());
                sentencia.addBatch();
            }
            sentencia.executeBatch();
        }
    }
}
