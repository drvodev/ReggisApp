package org.drvo.reggisapp.domain.service;

import org.drvo.reggisapp.domain.model.CampoCliente;
import org.drvo.reggisapp.domain.model.Cliente;

import java.util.List;
import java.util.Map;

public interface ClienteService {
    Cliente crear(String nombre);
    Cliente actualizar(Long clienteId, String nombre, Map<String, String> datosAdicionales);
    Cliente inactivar(Long clienteId);
    List<Cliente> listarActivos();
    CampoCliente crearCampo(String clave, String etiqueta);
    List<CampoCliente> listarCamposActivos();
}
