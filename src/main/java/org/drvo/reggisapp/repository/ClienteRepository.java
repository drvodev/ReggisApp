package org.drvo.reggisapp.repository;

import org.drvo.reggisapp.domain.model.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository {
    Cliente guardar(Cliente cliente);
    Optional<Cliente> buscarPorId(Long id);
    Cliente actualizar(Cliente cliente);
    List<Cliente> listarActivos();
}
