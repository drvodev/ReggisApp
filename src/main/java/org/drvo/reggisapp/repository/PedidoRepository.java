package org.drvo.reggisapp.repository;

import org.drvo.reggisapp.domain.model.Pedido;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository {
    Pedido guardar(Pedido pedido);
    Optional<Pedido> buscarPorId(Long id);
    Pedido actualizar(Pedido pedido);
    List<Pedido> listarPorCliente(Long clienteId);
}
