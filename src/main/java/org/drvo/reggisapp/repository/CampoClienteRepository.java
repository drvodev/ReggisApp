package org.drvo.reggisapp.repository;

import org.drvo.reggisapp.domain.model.CampoCliente;

import java.util.List;

public interface CampoClienteRepository {
    CampoCliente guardar(CampoCliente campo);
    List<CampoCliente> listarActivos();
}
