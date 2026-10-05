package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.model.Cliente;

import java.util.List;

public interface ReporteService {
    List<Cliente> seleccionarClientes(List<Long> clienteIds);
}
