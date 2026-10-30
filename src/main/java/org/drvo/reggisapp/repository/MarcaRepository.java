package org.drvo.reggisapp.repository;
import java.util.List;
import java.util.Set;
import org.drvo.reggisapp.domain.model.MarcaPedido;
public interface MarcaRepository {
    List<MarcaPedido> listar();
    void crear(String nombre);
    void guardarSeleccion(long pedidoId, Set<Long> seleccionadas);
    List<MarcaPedido> listarPorPedido(long pedidoId);
}
