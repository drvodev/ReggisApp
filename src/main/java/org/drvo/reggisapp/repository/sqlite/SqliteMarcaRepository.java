package org.drvo.reggisapp.repository.sqlite;
import org.drvo.reggisapp.config.ConnectionFactory;
import org.drvo.reggisapp.domain.model.MarcaPedido;
import org.drvo.reggisapp.domain.exception.ReglaNegocioException;
import org.drvo.reggisapp.repository.AbstractJdbcRepository;
import org.drvo.reggisapp.repository.MarcaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
public final class SqliteMarcaRepository extends AbstractJdbcRepository implements MarcaRepository {
    public SqliteMarcaRepository(ConnectionFactory conexiones) { super(conexiones); }
    public List<MarcaPedido> listar() { return consultar("SELECT id, nombre, 0 AS seleccionada FROM marcas WHERE activa = 1 ORDER BY nombre COLLATE NOCASE", null); }
    public List<MarcaPedido> listarPorPedido(long pedidoId) { return consultar("SELECT m.id, m.nombre, pm.seleccionada FROM pedido_marcas pm JOIN marcas m ON m.id=pm.marca_id WHERE pm.pedido_id=? ORDER BY m.nombre COLLATE NOCASE", pedidoId); }
    private List<MarcaPedido> consultar(String sql, Long pedidoId) {
        return withConnection(c -> { var lista = new ArrayList<MarcaPedido>();
            try (var s = c.prepareStatement(sql)) { if (pedidoId != null) s.setLong(1, pedidoId);
                try (var r = s.executeQuery()) { while (r.next()) lista.add(new MarcaPedido(r.getLong(1), r.getString(2), r.getInt(3) == 1)); }
            } return List.copyOf(lista); });
    }
    public void crear(String nombre) {
        if (nombre == null || nombre.isBlank()) throw new ReglaNegocioException("Escribe el nombre de la marca.");
        if (nombre.strip().length() > 100 || nombre.codePoints().anyMatch(Character::isISOControl))
            throw new ReglaNegocioException("El nombre de la marca debe tener hasta 100 caracteres y no contener caracteres de control.");
        withConnection(c -> { try (var s = c.prepareStatement("INSERT INTO marcas(nombre) VALUES (?) ON CONFLICT(nombre) DO UPDATE SET activa = 1 WHERE activa = 0")) {
            s.setString(1, nombre.strip()); if (s.executeUpdate() == 0) throw new ReglaNegocioException("Esta marca ya existe.");
        } return null; });
    }
    public void retirar(long id) {
        withConnection(c -> { try (var s = c.prepareStatement("UPDATE marcas SET activa = 0 WHERE id = ? AND activa = 1")) {
            s.setLong(1, id);
            if (s.executeUpdate() == 0) throw new ReglaNegocioException("La marca no está disponible.");
        } return null; });
    }
    public void guardarSeleccion(long pedidoId, Set<Long> seleccionadas) {
        var marcas = listar();
        if (!marcas.stream().map(MarcaPedido::id).collect(java.util.stream.Collectors.toSet()).containsAll(seleccionadas))
            throw new ReglaNegocioException("Una marca seleccionada ya no existe.");
        withConnection(c -> { try (var s = c.prepareStatement("INSERT INTO pedido_marcas(pedido_id, marca_id, seleccionada) VALUES(?,?,?)")) {
            for (var marca : marcas) { s.setLong(1, pedidoId); s.setLong(2, marca.id()); s.setInt(3, seleccionadas.contains(marca.id()) ? 1 : 0); s.addBatch(); } s.executeBatch();
        } return null; });
    }
}
