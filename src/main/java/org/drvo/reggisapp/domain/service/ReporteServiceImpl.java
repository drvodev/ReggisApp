package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.exception.ReglaNegocioException;
import org.drvo.reggisapp.domain.model.Cliente;
import org.drvo.reggisapp.repository.ClienteRepository;

import java.util.List;
import java.util.Objects;

public class ReporteServiceImpl implements ReporteService {
    private final ClienteRepository clienteRepository;

    public ReporteServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
    }

    @Override
    public List<Cliente> seleccionarClientes(List<Long> clienteIds) {
        if (clienteIds == null || clienteIds.isEmpty()) {
            throw new ReglaNegocioException("Seleccione al menos un cliente para el reporte.");
        }
        return clienteIds.stream().distinct()
                .map(id -> clienteRepository.buscarPorId(id)
                        .orElseThrow(() -> new ReglaNegocioException("No se encontró el cliente " + id + ".")))
                .toList();
    }
}
